package com.hnust.examai.module.auth.dto;

import lombok.Data;

/**
 * 登录响应体
 */
@Data
public class LoginResponse {

    /** JWT Token */
    private String token;

    /** 用户基本信息 */
    private UserInfoVO userInfo;

    @Data
    public static class UserInfoVO {
        private Long id;
        private String username;
        private String nickname;
        private String avatarUrl;
        /** 角色：0 普通用户，1 管理员 */
        private Integer role;
        private Integer studyStreak;
    }
}
