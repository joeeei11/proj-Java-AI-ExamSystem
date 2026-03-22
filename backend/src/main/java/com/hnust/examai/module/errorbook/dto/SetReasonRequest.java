package com.hnust.examai.module.errorbook.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 设置错因标签请求
 */
@Data
public class SetReasonRequest {

    /**
     * 错因标签：1 概念不清 / 2 审题失误 / 3 计算失误 / 4 方法不会 / 5 时间不足 / 6 粗心
     */
    @NotNull(message = "错因标签不能为空")
    @Min(value = 1, message = "错因标签值 1-6")
    @Max(value = 6, message = "错因标签值 1-6")
    private Integer reason;
}
