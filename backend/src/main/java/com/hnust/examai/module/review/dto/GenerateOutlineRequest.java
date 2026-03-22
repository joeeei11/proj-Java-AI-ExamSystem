package com.hnust.examai.module.review.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 生成复习提纲请求
 */
@Data
public class GenerateOutlineRequest {

    /** 来源笔记 ID 列表（至少选 1 篇） */
    @NotEmpty(message = "至少选择一篇笔记")
    private List<Long> noteIds;
}
