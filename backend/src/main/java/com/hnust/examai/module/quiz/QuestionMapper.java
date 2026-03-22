package com.hnust.examai.module.quiz;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnust.examai.entity.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 题目 Mapper
 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    /**
     * 批量插入题目，返回自增 ID
     */
    int batchInsert(@Param("list") List<Question> questions);
}
