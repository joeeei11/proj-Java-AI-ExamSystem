package com.hnust.examai.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 科目创建/更新请求体
 */
@Data
public class SubjectRequest {

    @NotBlank(message = "科目名称不能为空")
    @Size(max = 50, message = "科目名称最长 50 位")
    private String name;

    @Size(max = 200, message = "描述最长 200 位")
    private String description;

    private Integer sortOrder = 0;

    /** 是否启用：0 禁用，1 启用，默认 1 */
    private Integer isActive = 1;
}
