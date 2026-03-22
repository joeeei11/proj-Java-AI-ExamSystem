package com.hnust.examai.module.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 提交答案请求
 */
@Data
@Schema(description = "提交答案请求")
public class SubmitRequest {

    @NotNull(message = "sessionId 不能为空")
    @Schema(description = "答题会话 ID（前端生成的 UUID）", required = true)
    private String sessionId;

    @NotEmpty(message = "答题列表不能为空")
    @Valid
    @Schema(description = "答题列表", required = true)
    private List<AnswerItem> answers;

    /**
     * 单题答案
     */
    @Data
    public static class AnswerItem {

        @NotNull(message = "questionId 不能为空")
        private Long questionId;

        @NotNull(message = "answer 不能为空")
        private String answer;

        /** 作答耗时（秒，可选） */
        private Integer timeSpentS;
    }
}
