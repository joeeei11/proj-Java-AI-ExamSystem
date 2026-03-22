package com.hnust.examai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 抽认卡组实体（对应 t_flashcard_deck）
 */
@Data
@TableName("t_flashcard_deck")
public class FlashcardDeck {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 来源笔记 ID（可为 null） */
    private Long noteId;

    /** 卡组标题 */
    private String title;

    /** 卡片数量（冗余字段，需同步维护） */
    private Integer cardCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
