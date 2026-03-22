package com.hnust.examai.module.stats.dto;

import lombok.Data;

/**
 * 科目答题统计 VO
 */
@Data
public class SubjectStatVO {

    /** 科目 ID */
    private Long subjectId;

    /** 科目名称 */
    private String subjectName;

    /** 该科目总答题数 */
    private Long totalAnswered;

    /** 该科目正确数 */
    private Long correctCount;

    /** 该科目正确率（保留1位小数） */
    private Double accuracy;
}
