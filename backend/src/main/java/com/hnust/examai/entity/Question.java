package com.hnust.examai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目实体（对应 t_question）
 */
@Data
@TableName("t_question")
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long subjectId;

    private Long knowledgePointId;

    /**
     * 题型：1 单选 2 多选 3 判断 4 填空
     */
    private Integer type;

    /**
     * 难度：1 简单 2 中等 3 困难
     */
    private Integer difficulty;

    /** 题干 */
    private String content;

    /**
     * 选项（JSON 数组 [{key:"A", value:"..."}, ...]，填空题为 null）
     * MyBatis-Plus 自动处理 JSON 字段
     */
    private String options;

    /** 正确答案 */
    private String answer;

    /** AI 解析 */
    private String explanation;

    /**
     * 来源：1 AI 生成 2 管理员录入
     */
    private Integer source;

    /** AI 缓存键（相同参数复用缓存时记录） */
    private String aiCacheKey;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
