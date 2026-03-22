package com.hnust.examai.module.errorbook;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnust.examai.entity.ErrorBook;
import com.hnust.examai.module.errorbook.dto.ErrorBookVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 错题本 Mapper
 */
@Mapper
public interface ErrorBookMapper extends BaseMapper<ErrorBook> {

    /**
     * 插入错题记录，若已存在则更新 updated_at（防重）
     * 依赖 UNIQUE KEY uk_user_question(user_id, question_id)
     */
    int upsertError(@Param("userId") Long userId, @Param("questionId") Long questionId);

    /**
     * 查询用户错题列表（JOIN 题目表 + 科目表，支持过滤）
     *
     * @param userId     用户 ID
     * @param subjectId  科目 ID（null 表示不过滤）
     * @param isMastered 掌握状态（null 表示不过滤）
     */
    List<ErrorBookVO> selectByUserId(
            @Param("userId") Long userId,
            @Param("subjectId") Long subjectId,
            @Param("isMastered") Integer isMastered
    );

    /**
     * 查询今日 SRS 到期的错题（next_review_at <= NOW()，且未掌握）
     *
     * @param userId 用户 ID
     */
    List<ErrorBookVO> selectTodayReview(@Param("userId") Long userId);
}
