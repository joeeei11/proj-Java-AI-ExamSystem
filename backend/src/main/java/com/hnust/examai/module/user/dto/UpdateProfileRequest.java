package com.hnust.examai.module.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新个人资料请求体
 */
@Data
public class UpdateProfileRequest {

    @Size(max = 50, message = "昵称最长 50 位")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱最长 100 位")
    private String email;
}
