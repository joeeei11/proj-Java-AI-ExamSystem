package com.hnust.examai.module.feedback.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交题目纠错反馈请求
 */
@Data
public class SubmitFeedbackRequest {

    @NotNull(message = "题目 ID 不能为空")
    private Long questionId;

    /**
     * 反馈类型：1 答案错误 / 2 题干歧义 / 3 解析不清 / 4 排版问题
     */
    @NotNull(message = "反馈类型不能为空")
    @Min(value = 1, message = "反馈类型无效")
    @Max(value = 4, message = "反馈类型无效")
    private Integer type;

    @Size(max = 500, message = "描述最多 500 字")
    private String description;

    /** 截图 URL（可选，复用 MinIO 上传） */
    private String screenshotUrl;
}
