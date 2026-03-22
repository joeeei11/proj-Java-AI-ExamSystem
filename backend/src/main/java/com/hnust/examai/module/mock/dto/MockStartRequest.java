package com.hnust.examai.module.mock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 开始模考请求
 */
@Data
@Schema(description = "开始模考请求")
public class MockStartRequest {

    @NotNull
    @Schema(description = "科目ID", example = "1")
    private Long subjectId;

    @NotNull
    @Min(5)
    @Max(20)
    @Schema(description = "题目数量（5-20）", example = "10")
    private Integer count;

    @NotNull
    @Min(10)
    @Max(180)
    @Schema(description = "考试时长（分钟，10-180）", example = "30")
    private Integer timeLimitMinutes;

    @Schema(description = "难度（可选）1简单/2中等/3困难")
    private Integer difficulty;
}
