package com.hnust.examai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记实体（对应 t_note）
 */
@Data
@TableName("t_note")
public class Note {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long subjectId;

    /** 标题 */
    private String title;

    /** 内容（Markdown） */
    private String content;

    /** 原始图片 URL（OCR 笔记） */
    private String imageUrl;

    /** OCR 识别文本 */
    private String ocrText;

    /**
     * 来源：1 手动 2 OCR 3 AI整理
     */
    private Integer source;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
