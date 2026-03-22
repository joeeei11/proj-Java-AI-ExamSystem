package com.hnust.examai.module.flashcard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 编辑卡片请求（front/back）
 */
@Data
public class UpdateCardRequest {

    @NotBlank(message = "问题面不能为空")
    private String front;

    @NotBlank(message = "答案面不能为空")
    private String back;
}
