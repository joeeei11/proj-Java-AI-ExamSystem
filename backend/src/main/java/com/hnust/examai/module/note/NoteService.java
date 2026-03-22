package com.hnust.examai.module.note;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnust.examai.common.ai.DeepSeekClient;
import com.hnust.examai.common.exception.BizException;
import com.hnust.examai.common.result.ResultCode;
import com.hnust.examai.common.utils.MinioUtil;
import com.hnust.examai.common.utils.OcrClient;
import com.hnust.examai.entity.Note;
import com.hnust.examai.entity.Subject;
import com.hnust.examai.module.admin.SubjectMapper;
import com.hnust.examai.module.note.dto.NoteCreateRequest;
import com.hnust.examai.module.note.dto.NoteUpdateRequest;
import com.hnust.examai.module.note.dto.NoteVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 笔记 Service
 * <p>
 * 支持手动创建、图片 OCR 识别（MinIO + PaddleOCR）、AI 整理（DeepSeek）
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteMapper noteMapper;
    private final SubjectMapper subjectMapper;
    private final MinioUtil minioUtil;
    private final OcrClient ocrClient;
    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper;

    // ===== 公开方法 =====

    /**
     * 笔记列表（按更新时间倒序，支持科目过滤）
     */
    public List<NoteVO> list(Long userId, Long subjectId) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<Note>()
                .eq(Note::getUserId, userId)
                .eq(subjectId != null, Note::getSubjectId, subjectId)
                .orderByDesc(Note::getUpdatedAt);
        List<Note> notes = noteMapper.selectList(wrapper);
        return toVOList(notes);
    }

    /**
     * 获取单篇笔记（校验归属）
     */
    public NoteVO get(Long userId, Long noteId) {
        Note note = requireNote(noteId, userId);
        return toVO(note, fetchSubjectName(note.getSubjectId()));
    }

    /**
     * 手动创建笔记
     */
    public NoteVO create(Long userId, NoteCreateRequest req) {
        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(req.getTitle());
        note.setContent(req.getContent());
        note.setSubjectId(req.getSubjectId());
        note.setSource(1); // 手动
        noteMapper.insert(note);
        log.info("手动创建笔记，userId={}, noteId={}", userId, note.getId());
        return toVO(note, fetchSubjectName(note.getSubjectId()));
    }

    /**
     * 更新笔记（仅更新非 null 字段）
     */
    public NoteVO update(Long userId, Long noteId, NoteUpdateRequest req) {
        Note note = requireNote(noteId, userId);
        if (req.getTitle() != null) note.setTitle(req.getTitle());
        if (req.getContent() != null) note.setContent(req.getContent());
        if (req.getSubjectId() != null) note.setSubjectId(req.getSubjectId());
        noteMapper.updateById(note);
        return toVO(note, fetchSubjectName(note.getSubjectId()));
    }

    /**
     * 删除笔记（先删 MinIO 图片，再删数据库记录）
     */
    public void delete(Long userId, Long noteId) {
        Note note = requireNote(noteId, userId);
        // 同步删除 MinIO 图片（忽略失败）
        if (note.getImageUrl() != null) {
            String objectName = minioUtil.extractObjectName(note.getImageUrl());
            if (objectName != null) {
                minioUtil.delete(objectName);
            }
        }
        noteMapper.deleteById(noteId);
        log.info("删除笔记，userId={}, noteId={}", userId, noteId);
    }

    /**
     * 图片 OCR：multipart 上传 → MinIO → Base64 → OcrClient → 创建笔记
     *
     * @param file      上传的图片文件
     * @param subjectId 关联科目（可为 null）
     */
    public NoteVO ocr(Long userId, MultipartFile file, Long subjectId) {
        // 1. 上传 MinIO，保存原始图片
        String imageUrl = minioUtil.upload(file, "notes/");

        // 2. 读取文件字节并转 Base64（不经过 data URI，让 OcrClient 直接处理纯 Base64）
        byte[] imageBytes;
        try {
            imageBytes = file.getBytes();
        } catch (IOException e) {
            throw new BizException(ResultCode.OCR_FAILED, "图片读取失败: " + e.getMessage());
        }
        String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);

        // 3. 调用 OCR 微服务
        String ocrText = ocrClient.recognize(imageBase64);

        // 4. 创建笔记
        Note note = new Note();
        note.setUserId(userId);
        note.setTitle("OCR 笔记 " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")));
        note.setContent(ocrText);
        note.setImageUrl(imageUrl);
        note.setOcrText(ocrText);
        note.setSubjectId(subjectId);
        note.setSource(2); // OCR
        noteMapper.insert(note);

        log.info("OCR 笔记创建成功，userId={}, noteId={}, 文本长度={}", userId, note.getId(), ocrText.length());
        return toVO(note, fetchSubjectName(note.getSubjectId()));
    }

    /**
     * AI 整理笔记：调用 DeepSeek 将 ocrText/content 整理为 Markdown
     */
    public NoteVO organizeByAI(Long userId, Long noteId) {
        Note note = requireNote(noteId, userId);

        // 优先使用 ocrText（原始识别文本），否则用现有 content
        String rawContent = note.getOcrText() != null && !note.getOcrText().isBlank()
                ? note.getOcrText()
                : note.getContent();
        if (rawContent == null || rawContent.isBlank()) {
            throw new BizException(ResultCode.PARAM_ERROR, "笔记内容为空，无法整理");
        }

        // 加载 Prompt 模板
        String systemPrompt = loadPromptTemplate("note_organize.txt");

        // 调用 DeepSeek（DeepSeekClient 内部已含重试逻辑）
        String aiResponse = deepSeekClient.chat(systemPrompt, rawContent);

        // 解析响应（期望 { "markdown": "..." }）
        String organizedContent = parseOrganizeResponse(aiResponse, noteId);

        // 更新笔记内容和来源标记
        note.setContent(organizedContent);
        note.setSource(3); // AI 整理
        noteMapper.updateById(note);

        log.info("AI 整理笔记完成，userId={}, noteId={}", userId, noteId);
        return toVO(note, fetchSubjectName(note.getSubjectId()));
    }

    // ===== 私有方法 =====

    /**
     * 加载 Prompt 模板文件
     */
    private String loadPromptTemplate(String filename) {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/" + filename);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BizException(ResultCode.AI_GENERATE_FAILED, "Prompt 模板加载失败: " + filename);
        }
    }

    /**
     * 解析 AI 整理响应，提取 markdown 字段；降级直接返回原文
     */
    private String parseOrganizeResponse(String aiResponse, Long noteId) {
        try {
            JsonNode root = objectMapper.readTree(aiResponse);
            JsonNode markdownNode = root.path("markdown");
            if (!markdownNode.isMissingNode() && !markdownNode.isNull()) {
                return markdownNode.asText();
            }
            // Schema 校验失败，降级返回原始文本
            log.warn("AI 整理响应缺少 markdown 字段，noteId={}，降级使用原文", noteId);
            return aiResponse;
        } catch (Exception e) {
            log.warn("AI 整理响应解析失败，noteId={}，降级使用原文", noteId, e);
            return aiResponse;
        }
    }

    /**
     * 查询笔记并校验归属，不存在或无权限时抛出业务异常
     */
    private Note requireNote(Long noteId, Long userId) {
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new BizException(ResultCode.NOTE_NOT_FOUND);
        }
        if (!note.getUserId().equals(userId)) {
            throw new BizException(ResultCode.NOTE_ACCESS_DENIED);
        }
        return note;
    }

    /**
     * 通过科目 ID 查询科目名称（ID 为 null 时返回 null）
     */
    private String fetchSubjectName(Long subjectId) {
        if (subjectId == null) return null;
        Subject subject = subjectMapper.selectById(subjectId);
        return subject != null ? subject.getName() : null;
    }

    /**
     * 将单个 Note 实体转为 VO
     */
    private NoteVO toVO(Note note, String subjectName) {
        NoteVO vo = new NoteVO();
        vo.setId(note.getId());
        vo.setSubjectId(note.getSubjectId());
        vo.setSubjectName(subjectName);
        vo.setTitle(note.getTitle());
        vo.setContent(note.getContent());
        vo.setImageUrl(note.getImageUrl());
        vo.setOcrText(note.getOcrText());
        vo.setSource(note.getSource());
        vo.setCreatedAt(note.getCreatedAt());
        vo.setUpdatedAt(note.getUpdatedAt());
        return vo;
    }

    /**
     * 将 Note 列表转为 VO 列表（批量获取科目名称，避免 N+1 查询）
     */
    private List<NoteVO> toVOList(List<Note> notes) {
        if (notes.isEmpty()) return Collections.emptyList();

        // 批量获取所有涉及的科目
        Set<Long> subjectIds = notes.stream()
                .filter(n -> n.getSubjectId() != null)
                .map(Note::getSubjectId)
                .collect(Collectors.toSet());

        Map<Long, String> subjectNameMap = new HashMap<>();
        if (!subjectIds.isEmpty()) {
            subjectMapper.selectBatchIds(subjectIds)
                    .forEach(s -> subjectNameMap.put(s.getId(), s.getName()));
        }

        return notes.stream()
                .map(n -> toVO(n, subjectNameMap.get(n.getSubjectId())))
                .collect(Collectors.toList());
    }
}
