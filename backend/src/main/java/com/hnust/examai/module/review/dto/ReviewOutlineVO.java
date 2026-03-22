package com.hnust.examai.module.review.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 复习提纲视图对象
 */
@Data
public class ReviewOutlineVO {

    private Long id;

    private String title;

    /** 提纲内容（Markdown） */
    private String content;

    /** 来源笔记 ID 列表 */
    private List<Long> noteIds;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
