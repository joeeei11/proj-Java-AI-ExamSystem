package com.hnust.examai.module.flashcard.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 抽认卡组 VO
 */
@Data
public class FlashcardDeckVO {

    private Long id;

    /** 来源笔记 ID */
    private Long noteId;

    /** 卡组标题 */
    private String title;

    /** 卡片数量 */
    private Integer cardCount;

    /** 今日待复习数量 */
    private Integer todayReviewCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
