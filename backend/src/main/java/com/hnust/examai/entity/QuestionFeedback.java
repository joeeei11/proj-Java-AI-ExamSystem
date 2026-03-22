package com.hnust.examai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目纠错反馈实体（对应 t_question_feedback）
 */
@Data
@TableName("t_question_feedback")
public class QuestionFeedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long questionId;

    /**
     * 反馈类型：1 答案错误 / 2 题干歧义 / 3 解析不清 / 4 排版问题
     */
    private Integer type;

    private String description;

    /** 截图 URL（可选） */
    private String screenshotUrl;

    /**
     * 处理状态：0 待处理 / 1 已采纳 / 2 已驳回 / 3 已修复
     */
    private Integer status;

    /** 处理管理员 ID */
    private Long adminId;

    /** 管理员回复 */
    private String adminReply;

    private LocalDateTime repliedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
