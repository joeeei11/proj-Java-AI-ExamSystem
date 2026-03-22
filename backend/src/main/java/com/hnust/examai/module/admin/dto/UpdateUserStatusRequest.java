package com.hnust.examai.module.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新用户状态请求（启用/禁用）
 */
@Data
public class UpdateUserStatusRequest {

    /** 是否启用：true 启用，false 禁用 */
    @NotNull(message = "isActive 不能为空")
    private Boolean isActive;
}
