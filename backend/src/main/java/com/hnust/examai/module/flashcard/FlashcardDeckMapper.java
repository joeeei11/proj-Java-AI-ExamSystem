package com.hnust.examai.module.flashcard;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnust.examai.entity.FlashcardDeck;
import org.apache.ibatis.annotations.Mapper;

/**
 * 抽认卡组 Mapper
 */
@Mapper
public interface FlashcardDeckMapper extends BaseMapper<FlashcardDeck> {
}
