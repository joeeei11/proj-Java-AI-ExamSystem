package com.hnust.examai.module.mock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 模考交卷请求
 */
@Data
@Schema(description = "模考交卷请求")
public class MockSubmitRequest {

    @NotNull
    @Schema(description = "答案列表")
    private List<AnswerItem> answers;

    /**
     * 单题答案
     */
    @Data
    public static class AnswerItem {

        @NotNull
        @Schema(description = "题目ID")
        private Long questionId;

        @Schema(description = "用户答案（未作答传null）")
        private String answer;
    }
}
