package com.hnust.examai.module.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 题目视图对象（前端展示用，含 options 列表）
 */
@Data
@Schema(description = "题目视图")
public class QuestionVO {

    @Schema(description = "题目 ID")
    private Long id;

    @Schema(description = "科目 ID")
    private Long subjectId;

    @Schema(description = "知识点 ID")
    private Long knowledgePointId;

    /**
     * 题型：1 单选 2 多选 3 判断 4 填空
     */
    @Schema(description = "题型")
    private Integer type;

    @Schema(description = "难度 1-3")
    private Integer difficulty;

    @Schema(description = "题干")
    private String content;

    /**
     * 选项列表（JSON 数组反序列化后）
     */
    @Schema(description = "选项列表 [{key:'A', value:'...'}]")
    private List<Option> options;

    /**
     * 正确答案（提交后才返回）
     */
    @Schema(description = "正确答案")
    private String answer;

    /**
     * 解析（提交后才返回）
     */
    @Schema(description = "解析")
    private String explanation;

    /**
     * 选项结构
     */
    @Data
    public static class Option {
        private String key;
        private String value;
    }
}
