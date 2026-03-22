package com.hnust.examai.module.feedback.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员处理反馈请求
 */
@Data
public class HandleFeedbackRequest {

    /**
     * 处理状态：1 已采纳 / 2 已驳回 / 3 已修复
     */
    @NotNull(message = "处理状态不能为空")
    @Min(value = 1, message = "状态值无效")
    @Max(value = 3, message = "状态值无效")
    private Integer status;

    @Size(max = 500, message = "回复最多 500 字")
    private String adminReply;
}
