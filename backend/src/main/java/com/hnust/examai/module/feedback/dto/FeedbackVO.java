package com.hnust.examai.module.feedback.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 反馈列表项 VO（含题目简要信息）
 */
@Data
public class FeedbackVO {

    private Long id;

    private Long questionId;

    /** 题目内容摘要（前 60 字） */
    private String questionSummary;

    /**
     * 反馈类型：1 答案错误 / 2 题干歧义 / 3 解析不清 / 4 排版问题
     */
    private Integer type;

    private String typeLabel;

    /**
     * 处理状态：0 待处理 / 1 已采纳 / 2 已驳回 / 3 已修复
     */
    private Integer status;

    private String statusLabel;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime repliedAt;
}
