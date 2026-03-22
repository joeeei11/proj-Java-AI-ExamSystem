package com.hnust.examai.module.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员查看的用户信息 VO
 */
@Data
public class UserAdminVO {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    /** 角色：0 普通用户，1 管理员 */
    private Integer role;

    /** 是否启用：0 禁用，1 正常 */
    private Integer isActive;

    private Integer studyStreak;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;
}
