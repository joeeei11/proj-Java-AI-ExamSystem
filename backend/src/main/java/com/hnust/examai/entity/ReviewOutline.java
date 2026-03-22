package com.hnust.examai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 复习提纲实体（对应 t_review_outline）
 */
@Data
@TableName("t_review_outline")
public class ReviewOutline {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 提纲标题（AI 自动生成） */
    private String title;

    /** 提纲内容（Markdown） */
    private String content;

    /**
     * 来源笔记 ID 列表（JSON 数组字符串，如 "[1,2,3]"）
     */
    private String noteIds;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
