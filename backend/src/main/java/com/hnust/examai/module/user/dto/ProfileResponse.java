package com.hnust.examai.module.user.dto;

import lombok.Data;

/**
 * 用户个人资料响应体
 */
@Data
public class ProfileResponse {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatarUrl;
    /** 角色：0 普通用户，1 管理员 */
    private Integer role;
    private Integer studyStreak;
    private String lastLoginAt;
    private String createdAt;
}
