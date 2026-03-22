package com.hnust.examai.module.flashcard.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 抽认卡 VO
 */
@Data
public class FlashcardVO {

    private Long id;

    private Long deckId;

    /** 问题面 */
    private String front;

    /** 答案面 */
    private String back;

    private Integer sortOrder;

    /** 总复习次数 */
    private Integer reviewCount;

    /** 连续正确次数 */
    private Integer consecutiveCorrect;

    /** 当前间隔天数 */
    private Integer reviewIntervalDays;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextReviewAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
