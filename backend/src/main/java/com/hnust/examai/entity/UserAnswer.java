package com.hnust.examai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户答题记录实体（对应 t_user_answer）
 */
@Data
@TableName("t_user_answer")
public class UserAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long questionId;

    /** 答题会话 ID（前端 UUID，聚合同一轮答题） */
    private String sessionId;

    /** 用户实际作答 */
    private String userAnswer;

    /** 是否正确：0 错误 1 正确 */
    private Integer isCorrect;

    /** 作答耗时（秒，可选） */
    private Integer timeSpentS;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
