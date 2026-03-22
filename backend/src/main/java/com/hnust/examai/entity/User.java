package com.hnust.examai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("t_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** BCrypt 加密后的密码，查询时注意排除 */
    private String password;

    private String nickname;

    private String email;

    private String avatarUrl;

    /** 角色：0 普通用户，1 管理员 */
    private Integer role;

    /** 是否启用：0 禁用，1 正常 */
    private Integer isActive;

    /** 连续打卡天数 */
    private Integer studyStreak;

    private LocalDateTime lastLoginAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
