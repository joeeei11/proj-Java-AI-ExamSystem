package com.hnust.examai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 模考题目明细实体（对应 t_mock_exam_question）
 */
@Data
@TableName("t_mock_exam_question")
public class MockExamQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long examId;

    private Long questionId;

    private Integer sortOrder;

    /** 是否标记"未确定"：0 未标记 1 已标记 */
    private Integer isFlagged;

    /** 用户作答（交卷前可修改） */
    private String userAnswer;

    /** 是否正确（交卷后写入） */
    private Integer isCorrect;
}
