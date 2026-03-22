package com.hnust.examai.module.mock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 模考答题视图（答题过程中，不含正确答案和解析）
 */
@Data
@Schema(description = "模考答题视图")
public class MockExamVO {

    @Schema(description = "模考ID")
    private Long examId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "题目总数")
    private Integer totalQuestions;

    @Schema(description = "剩余秒数")
    private Long remainingSeconds;

    @Schema(description = "状态：0进行中 1已完成 2超时")
    private Integer status;

    @Schema(description = "题目列表（不含答案）")
    private List<MockQuestionItem> questions;

    /**
     * 模考题目项（含是否标记、用户当前作答）
     */
    @Data
    public static class MockQuestionItem {

        @Schema(description = "题目ID")
        private Long questionId;

        @Schema(description = "序号（从1开始）")
        private Integer sortOrder;

        @Schema(description = "是否标记未确定")
        private Boolean flagged;

        @Schema(description = "用户当前作答（可能为null）")
        private String userAnswer;

        @Schema(description = "题干")
        private String content;

        @Schema(description = "题型：1单选 2多选 3判断 4填空")
        private Integer type;

        @Schema(description = "难度")
        private Integer difficulty;

        @Schema(description = "选项列表")
        private List<OptionItem> options;
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
