package com.hnust.examai.module.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新笔记请求（所有字段可选，仅更新非 null 字段）
 */
@Data
@Schema(description = "更新笔记请求")
public class NoteUpdateRequest {

    @Size(max = 200, message = "标题最长 200 字符")
    @Schema(description = "笔记标题")
    private String title;

    @Schema(description = "笔记内容（Markdown）")
    private String content;

    @Schema(description = "关联科目 ID")
    private Long subjectId;
}
