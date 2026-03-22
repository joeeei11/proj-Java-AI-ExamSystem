package com.hnust.examai.module.note;

import com.hnust.examai.common.result.R;
import com.hnust.examai.module.note.dto.NoteCreateRequest;
import com.hnust.examai.module.note.dto.NoteUpdateRequest;
import com.hnust.examai.module.note.dto.NoteVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 笔记接口
 */
@Tag(name = "笔记", description = "手动创建、OCR 识别、AI 整理笔记")
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    /**
     * 笔记列表（支持科目过滤）
     */
    @Operation(summary = "笔记列表")
    @GetMapping
    public R<List<NoteVO>> list(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long subjectId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return R.ok(noteService.list(userId, subjectId));
    }

    /**
     * 笔记详情
     */
    @Operation(summary = "笔记详情")
    @GetMapping("/{id}")
    public R<NoteVO> get(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return R.ok(noteService.get(userId, id));
    }

    /**
     * 手动创建笔记
     */
    @Operation(summary = "手动创建笔记")
    @PostMapping
    public R<NoteVO> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody NoteCreateRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return R.ok(noteService.create(userId, request));
    }

    /**
     * 更新笔记
     */
    @Operation(summary = "更新笔记")
    @PutMapping("/{id}")
    public R<NoteVO> update(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody NoteUpdateRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return R.ok(noteService.update(userId, id, request));
    }

    /**
     * 删除笔记
     */
    @Operation(summary = "删除笔记")
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = Long.parseLong(userDetails.getUsername());
        noteService.delete(userId, id);
        return R.ok(null);
    }

    /**
     * 图片 OCR：multipart/form-data 上传图片，识别文字并创建笔记
     */
    @Operation(summary = "图片 OCR 识别并创建笔记")
    @PostMapping(value = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<NoteVO> ocr(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "subjectId", required = false) Long subjectId) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return R.ok(noteService.ocr(userId, file, subjectId));
    }

    /**
     * AI 整理笔记内容（将 ocrText/content 整理为 Markdown）
     */
    @Operation(summary = "AI 整理笔记内容")
    @PostMapping("/{id}/organize")
    public R<NoteVO> organize(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return R.ok(noteService.organizeByAI(userId, id));
    }
}
