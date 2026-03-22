package com.hnust.examai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学习统计汇总实体（按天）
 */
@Data
@TableName("t_study_stat")
public class StudyStat {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate statDate;

    private Integer answerCount;

    private Integer correctCount;

    private Integer studyMinutes;

    /** 是否打卡：0 未打卡，1 已打卡 */
    private Integer checkin;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
