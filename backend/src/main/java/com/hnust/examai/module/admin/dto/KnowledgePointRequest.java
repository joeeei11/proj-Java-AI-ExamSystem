package com.hnust.examai.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识点创建/更新请求体
 */
@Data
public class KnowledgePointRequest {

    @NotNull(message = "科目 ID 不能为空")
    private Long subjectId;

    @NotBlank(message = "知识点名称不能为空")
    @Size(max = 100, message = "知识点名称最长 100 位")
    private String name;

    private String description;

    private Integer sortOrder = 0;
}
