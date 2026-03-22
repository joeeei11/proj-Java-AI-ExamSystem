package com.hnust.examai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 抽认卡实体（对应 t_flashcard）
 */
@Data
@TableName("t_flashcard")
public class Flashcard {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long deckId;

    private Long userId;

    /** 问题面 */
    private String front;

    /** 答案面 */
    private String back;

    private Integer sortOrder;

    /** SRS 下次复习时间（null 表示从未复习） */
    private LocalDateTime nextReviewAt;

    /** 当前间隔天数 */
    private Integer reviewIntervalDays;

    /** 总复习次数 */
    private Integer reviewCount;

    /** 连续正确次数 */
    private Integer consecutiveCorrect;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
