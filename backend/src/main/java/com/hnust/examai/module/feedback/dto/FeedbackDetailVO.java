package com.hnust.examai.module.feedback.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 反馈详情 VO（完整信息，含管理员回复）
 */
@Data
public class FeedbackDetailVO {

    private Long id;

    private Long questionId;

    /** 题目完整内容 */
    private String questionContent;

    /** 题目答案 */
    private String questionAnswer;

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

    private String screenshotUrl;

    /** 管理员回复 */
    private String adminReply;

    private LocalDateTime createdAt;

    private LocalDateTime repliedAt;
}
