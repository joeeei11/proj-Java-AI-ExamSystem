package com.hnust.examai.module.mock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 标记/取消标记请求
 */
@Data
@Schema(description = "标记请求")
public class FlagRequest {

    @NotNull
    @Schema(description = "题目ID")
    private Long questionId;

    @NotNull
    @Schema(description = "是否标记")
    private Boolean flagged;
}
