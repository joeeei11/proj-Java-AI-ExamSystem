package com.hnust.examai.module.flashcard;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnust.examai.entity.Flashcard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 抽认卡 Mapper
 */
@Mapper
public interface FlashcardMapper extends BaseMapper<Flashcard> {

    /**
     * 查询今日到期待复习卡片（next_review_at <= now() 或首次未复习，按卡组过滤）
     *
     * @param userId 用户 ID
     * @return 今日待复习卡片列表
     */
    List<Flashcard> selectTodayReview(@Param("userId") Long userId);
}
