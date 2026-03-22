package com.hnust.examai.module.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 提交答案结果（含逐题正误 + AI 解析）
 */
@Data
@Schema(description = "提交答案结果")
public class SubmitResult {

    @Schema(description = "总题目数")
    private int totalCount;

    @Schema(description = "正确数")
    private int correctCount;

    @Schema(description = "正确率 0.0-1.0")
    private double accuracy;

    @Schema(description = "逐题结果")
    private List<QuestionResult> results;

    /**
     * 单题判分结果
     */
    @Data
    public static class QuestionResult {

        private Long questionId;

        @Schema(description = "用户答案")
        private String userAnswer;

        @Schema(description = "正确答案")
        private String correctAnswer;

        @Schema(description = "是否正确")
        private boolean correct;

        @Schema(description = "AI 解析")
        private String explanation;

        @Schema(description = "题型")
        private Integer type;

        @Schema(description = "题干（方便前端展示）")
        private String content;

        @Schema(description = "选项列表")
        private List<QuestionVO.Option> options;
    }
}
