package com.hnust.examai.module.quiz;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnust.examai.entity.UserAnswer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户答题记录 Mapper
 */
@Mapper
public interface UserAnswerMapper extends BaseMapper<UserAnswer> {
}
