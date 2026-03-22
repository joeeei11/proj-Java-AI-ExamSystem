package com.hnust.examai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模考记录实体（对应 t_mock_exam）
 */
@Data
@TableName("t_mock_exam")
public class MockExam {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long subjectId;

    /** 模考标题，如"行测模考 #3" */
    private String title;

    /** 考试时长（分钟） */
    private Integer timeLimitMin;

    /** 题目总数 */
    private Integer totalQuestions;

    /** 正确数 */
    private Integer totalCorrect;

    /**
     * 状态：0 进行中 1 已完成 2 超时
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime startedAt;

    private LocalDateTime submittedAt;
}
