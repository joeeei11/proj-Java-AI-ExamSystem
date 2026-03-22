package com.hnust.examai.module.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * AI 出题请求
 */
@Data
@Schema(description = "AI 出题请求")
public class GenerateRequest {

    @NotNull(message = "科目不能为空")
    @Schema(description = "科目 ID", required = true)
    private Long subjectId;

    @Min(value = 1, message = "难度范围 1-3")
    @Max(value = 3, message = "难度范围 1-3")
    @Schema(description = "难度 1:简单 2:中等 3:困难", defaultValue = "2")
    private Integer difficulty = 2;

    @Min(value = 1, message = "题目数量 1-20")
    @Max(value = 20, message = "题目数量 1-20")
    @Schema(description = "生成题目数量", defaultValue = "5")
    private Integer count = 5;

    @Schema(description = "指定知识点 ID 列表（可选）")
    private List<Long> knowledgePointIds;
}
