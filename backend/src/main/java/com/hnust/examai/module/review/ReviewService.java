package com.hnust.examai.module.review;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnust.examai.common.ai.DeepSeekClient;
import com.hnust.examai.common.exception.BizException;
import com.hnust.examai.common.result.ResultCode;
import com.hnust.examai.entity.Note;
import com.hnust.examai.entity.ReviewOutline;
import com.hnust.examai.module.note.NoteMapper;
import com.hnust.examai.module.review.dto.GenerateOutlineRequest;
import com.hnust.examai.module.review.dto.ReviewOutlineVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 复习提纲 Service
 * <p>
 * 基于用户笔记调用 DeepSeek 生成结构化 Markdown 复习提纲
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewOutlineMapper reviewOutlineMapper;
    private final NoteMapper noteMapper;
    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper;

    // ===== 公开方法 =====

    /**
     * 生成复习提纲
     * <p>
     * 1. 读取所选笔记内容 → 2. 拼接为 Prompt → 3. 调用 DeepSeek → 4. 保存提纲
     * </p>
     *
     * @param userId  当前用户 ID
     * @param request 请求参数（含 noteIds）
     * @return 生成的提纲 VO
     */
    public ReviewOutlineVO generateOutline(Long userId, GenerateOutlineRequest request) {
        // 1. 读取笔记内容（仅限当前用户）
        List<Note> notes = noteMapper.selectList(
                new LambdaQueryWrapper<Note>()
                        .in(Note::getId, request.getNoteIds())
                        .eq(Note::getUserId, userId)
        );

        if (notes.isEmpty()) {
            throw new BizException(ResultCode.REVIEW_NOTE_EMPTY);
        }

        // 2. 拼接笔记内容作为 Prompt 输入
        String combinedContent = notes.stream()
                .filter(n -> n.getContent() != null && !n.getContent().isBlank())
                .map(n -> "【" + n.getTitle() + "】\n" + n.getContent())
                .collect(Collectors.joining("\n\n---\n\n"));

        if (combinedContent.isBlank()) {
            throw new BizException(ResultCode.REVIEW_NOTE_EMPTY);
        }

        // 3. 调用 DeepSeek 生成提纲
        String systemPrompt = loadPromptTemplate("review_outline.txt");
        String aiResponse = deepSeekClient.chat(systemPrompt, combinedContent);

        // 4. 解析 AI 响应
        String title = parseOutlineTitle(aiResponse, notes);
        String content = parseOutlineContent(aiResponse);

        // 5. 序列化 noteIds 为 JSON 字符串
        String noteIdsJson = serializeNoteIds(request.getNoteIds());

        // 6. 保存到数据库
        ReviewOutline outline = new ReviewOutline();
        outline.setUserId(userId);
        outline.setTitle(title);
        outline.setContent(content);
        outline.setNoteIds(noteIdsJson);
        reviewOutlineMapper.insert(outline);

        log.info("复习提纲生成成功，userId={}, outlineId={}, noteCount={}",
                userId, outline.getId(), notes.size());
        return toVO(outline);
    }

    /**
     * 提纲列表（按创建时间倒序）
     *
     * @param userId 当前用户 ID
     */
    public List<ReviewOutlineVO> listOutlines(Long userId) {
        List<ReviewOutline> outlines = reviewOutlineMapper.selectList(
                new LambdaQueryWrapper<ReviewOutline>()
                        .eq(ReviewOutline::getUserId, userId)
                        .orderByDesc(ReviewOutline::getCreatedAt)
        );
        return outlines.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 删除提纲（校验归属）
     *
     * @param userId    当前用户 ID
     * @param outlineId 提纲 ID
     */
    public void deleteOutline(Long userId, Long outlineId) {
        ReviewOutline outline = requireOutline(outlineId, userId);
        reviewOutlineMapper.deleteById(outline.getId());
        log.info("删除复习提纲，userId={}, outlineId={}", userId, outlineId);
    }

    // ===== 私有方法 =====

    /**
     * 加载 Prompt 模板
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
     * 解析 AI 响应中的提纲标题
     */
    private String parseOutlineTitle(String aiResponse, List<Note> notes) {
        try {
            JsonNode root = objectMapper.readTree(aiResponse);
            JsonNode titleNode = root.path("title");
            if (!titleNode.isMissingNode() && !titleNode.isNull()) {
                return titleNode.asText();
            }
        } catch (Exception e) {
            log.warn("AI 响应标题解析失败，使用默认标题", e);
        }
        // 降级：使用第一篇笔记标题
        return notes.isEmpty() ? "复习提纲" : notes.get(0).getTitle() + " 复习提纲";
    }

    /**
     * 解析 AI 响应中的提纲 Markdown 内容
     */
    private String parseOutlineContent(String aiResponse) {
        try {
            JsonNode root = objectMapper.readTree(aiResponse);
            JsonNode contentNode = root.path("content");
            if (!contentNode.isMissingNode() && !contentNode.isNull()) {
                return contentNode.asText();
            }
        } catch (Exception e) {
            log.warn("AI 响应内容解析失败，降级使用原文", e);
        }
        return aiResponse;
    }

    /**
     * 将 noteIds 列表序列化为 JSON 字符串
     */
    private String serializeNoteIds(List<Long> noteIds) {
        try {
            return objectMapper.writeValueAsString(noteIds);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    /**
     * 将 noteIds JSON 字符串反序列化为列表
     */
    private List<Long> deserializeNoteIds(String noteIdsJson) {
        if (noteIdsJson == null || noteIdsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(
                    noteIdsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class)
            );
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * 查询提纲并校验归属
     */
    private ReviewOutline requireOutline(Long outlineId, Long userId) {
        ReviewOutline outline = reviewOutlineMapper.selectById(outlineId);
        if (outline == null) {
            throw new BizException(ResultCode.REVIEW_OUTLINE_NOT_FOUND);
        }
        if (!outline.getUserId().equals(userId)) {
            throw new BizException(ResultCode.REVIEW_OUTLINE_ACCESS_DENIED);
        }
        return outline;
    }

    /**
     * 将 ReviewOutline 实体转为 VO
     */
    private ReviewOutlineVO toVO(ReviewOutline outline) {
        ReviewOutlineVO vo = new ReviewOutlineVO();
        vo.setId(outline.getId());
        vo.setTitle(outline.getTitle());
        vo.setContent(outline.getContent());
        vo.setNoteIds(deserializeNoteIds(outline.getNoteIds()));
        vo.setCreatedAt(outline.getCreatedAt());
        vo.setUpdatedAt(outline.getUpdatedAt());
        return vo;
    }
}
