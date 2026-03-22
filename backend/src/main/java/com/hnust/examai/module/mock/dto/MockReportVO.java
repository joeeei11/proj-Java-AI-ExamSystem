package com.hnust.examai.module.mock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 模考报告 VO（含完整答案和解析）
 */
@Data
@Schema(description = "模考报告")
public class MockReportVO {

    @Schema(description = "模考ID")
    private Long examId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "总题数")
    private Integer totalQuestions;

    @Schema(description = "正确数")
    private Integer totalCorrect;

    @Schema(description = "正确率（0-1）")
    private Double accuracy;

    @Schema(description = "用时（秒）")
    private Long durationSeconds;

    @Schema(description = "状态：1已完成 2超时")
    private Integer status;

    @Schema(description = "逐题结果")
    private List<QuestionResultItem> questions;

    /**
     * 单题结果（含答案和解析）
     */
    @Data
    public static class QuestionResultItem {

        private Long questionId;
        private Integer sortOrder;
        private String content;
        private Integer type;
        private List<OptionItem> options;
        private String userAnswer;
        private String correctAnswer;
        private Boolean correct;
        private String explanation;
    }

    /**
     * 选项
     */
    @Data
    public static class OptionItem {
        private String key;
        private String value;
    }
}
