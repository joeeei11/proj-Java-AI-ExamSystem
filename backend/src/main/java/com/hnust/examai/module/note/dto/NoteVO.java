package com.hnust.examai.module.note.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记视图（包含科目名称）
 */
@Data
@Schema(description = "笔记视图")
public class NoteVO {

    @Schema(description = "笔记 ID")
    private Long id;

    @Schema(description = "关联科目 ID")
    private Long subjectId;

    @Schema(description = "关联科目名称")
    private String subjectName;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容（Markdown）")
    private String content;

    @Schema(description = "原始图片 URL（OCR 笔记）")
    private String imageUrl;

    @Schema(description = "OCR 识别原文")
    private String ocrText;

    /**
     * 来源：1 手动 2 OCR 3 AI整理
     */
    @Schema(description = "来源 1:手动 2:OCR 3:AI整理")
    private Integer source;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
