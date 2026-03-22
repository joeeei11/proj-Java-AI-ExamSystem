package com.hnust.examai.module.admin.dto;

import lombok.Data;

/**
 * 系统级统计总览 VO（管理员专用）
 */
@Data
public class SystemStatsVO {

    /** 注册用户总数 */
    private Long totalUsers;

    /** 题目总数 */
    private Long totalQuestions;

    /** 全平台答题总数 */
    private Long totalAnswers;

    /** 笔记总数 */
    private Long totalNotes;

    /** 今日活跃用户数（今日有答题记录的用户） */
    private Long todayActiveUsers;
}
