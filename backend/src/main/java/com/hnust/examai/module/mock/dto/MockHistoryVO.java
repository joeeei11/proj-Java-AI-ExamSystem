package com.hnust.examai.module.mock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 历史模考列表项 VO
 */
@Data
@Schema(description = "历史模考列表项")
public class MockHistoryVO {

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

    @Schema(description = "考试时长（分钟）")
    private Integer timeLimitMin;

    @Schema(description = "状态：0进行中 1已完成 2超时")
    private Integer status;

    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "提交时间")
    private LocalDateTime submittedAt;
}
