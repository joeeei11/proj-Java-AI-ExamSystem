package com.hnust.examai.module.flashcard.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 卡片复习结果提交请求
 */
@Data
public class FlashcardReviewRequest {

    /** true=会了，false=不会 */
    @NotNull(message = "known 字段不能为空")
    private Boolean known;
}
