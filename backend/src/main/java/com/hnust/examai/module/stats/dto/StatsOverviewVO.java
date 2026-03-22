package com.hnust.examai.module.stats.dto;

import lombok.Data;

/**
 * 学习统计总览 VO
 */
@Data
public class StatsOverviewVO {

    /** 累计答题数 */
    private Long totalAnswered;

    /** 累计正确数 */
    private Long totalCorrect;

    /** 正确率（保留1位小数，如 82.5 表示 82.5%） */
    private Double accuracy;

    /** 学习总天数（有答题记录的天数） */
    private Long studyDays;

    /** 当前连续打卡天数 */
    private Integer studyStreak;

    /** 错题总数 */
    private Long totalErrors;

    /** 已掌握错题数 */
    private Long masteredErrors;
}
