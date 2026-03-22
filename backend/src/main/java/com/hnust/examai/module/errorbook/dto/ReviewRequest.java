package com.hnust.examai.module.errorbook.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交错题复做结果请求
 */
@Data
public class ReviewRequest {

    /** 是否回答正确（已掌握） */
    @NotNull(message = "known 不能为空")
    private Boolean known;
}
