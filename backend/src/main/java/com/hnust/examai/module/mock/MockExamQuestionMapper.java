package com.hnust.examai.module.mock;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnust.examai.entity.MockExamQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 模考题目明细 Mapper
 */
@Mapper
public interface MockExamQuestionMapper extends BaseMapper<MockExamQuestion> {

    /**
     * 查询模考的所有题目明细，按序号排序
     */
    List<MockExamQuestion> selectByExamId(@Param("examId") Long examId);

    /**
     * 切换标记状态
     */
    int toggleFlag(@Param("examId") Long examId, @Param("questionId") Long questionId);
}
