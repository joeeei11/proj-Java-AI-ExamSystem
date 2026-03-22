package com.hnust.examai.module.flashcard.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 生成抽认卡请求
 */
@Data
public class GenerateFlashcardRequest {

    /** 来源笔记 ID */
    @NotNull(message = "笔记 ID 不能为空")
    private Long noteId;
}
