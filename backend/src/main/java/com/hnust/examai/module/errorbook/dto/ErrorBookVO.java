package com.hnust.examai.module.errorbook.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 错题本视图对象（含题目信息 + SRS 调度字段）
 */
@Data
public class ErrorBookVO {

    /** 错题本记录 ID */
    private Long id;

    /** 题目 ID */
    private Long questionId;

    // ===== 题目基本信息（JOIN t_question） =====

    /** 题型：1 单选 2 多选 3 判断 4 填空 */
    private Integer questionType;

    /** 难度：1 简单 2 中等 3 困难 */
    private Integer difficulty;

    /** 题干 */
    private String questionContent;

    /** 选项 JSON（选择题，填空题为 null） */
    private String options;

    /** 正确答案 */
    private String answer;

    /** AI 解析 */
    private String explanation;

    /** 科目 ID */
    private Long subjectId;

    /** 科目名称（JOIN t_subject） */
    private String subjectName;

    // ===== 错题本字段 =====

    /**
     * 错因标签：1 概念不清 / 2 审题失误 / 3 计算失误 / 4 方法不会 / 5 时间不足 / 6 粗心
     */
    private Integer errorReason;

    /** 是否已掌握：0 未掌握 1 已掌握 */
    private Integer isMastered;

    // ===== SRS 字段 =====

    private Integer reviewCount;

    private Integer consecutiveCorrect;

    /** 当前间隔天数 */
    private Integer reviewIntervalDays;

    /** 下次复习时间（null 表示从未复做） */
    private LocalDateTime nextReviewAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
