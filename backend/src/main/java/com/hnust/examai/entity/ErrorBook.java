package com.hnust.examai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 错题本实体（对应 t_error_book）
 */
@Data
@TableName("t_error_book")
public class ErrorBook {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long questionId;

    /**
     * 错因标签：1 概念不清 / 2 审题失误 / 3 计算失误 / 4 方法不会 / 5 时间不足 / 6 粗心
     */
    private Integer errorReason;

    /** 是否已掌握：0 未掌握 1 已掌握 */
    private Integer isMastered;

    private Integer reviewCount;

    private Integer consecutiveCorrect;

    /** 间隔复习天数 */
    private Integer reviewIntervalDays;

    /** SRS 下次复习时间 */
    private LocalDateTime nextReviewAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
