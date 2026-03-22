package com.hnust.examai.module.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 日度答题统计 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyStatVO {

    /** 日期（yyyy-MM-dd） */
    private String date;

    /** 当日答题数 */
    private Integer totalAnswered;

    /** 当日正确数 */
    private Integer correctCount;

    /** 当日正确率（保留1位小数） */
    private Double accuracy;
}
