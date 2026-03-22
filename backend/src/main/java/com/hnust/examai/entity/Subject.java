package com.hnust.examai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 科目实体
 */
@Data
@TableName("t_subject")
public class Subject {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private Integer sortOrder;

    /** 是否启用：0 禁用，1 启用 */
    private Integer isActive;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
