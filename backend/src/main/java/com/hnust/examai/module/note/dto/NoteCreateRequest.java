package com.hnust.examai.module.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新建笔记请求
 */
@Data
@Schema(description = "新建笔记请求")
public class NoteCreateRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最长 200 字符")
    @Schema(description = "笔记标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "笔记内容（Markdown）")
    private String content;

    @Schema(description = "关联科目 ID")
    private Long subjectId;
}
