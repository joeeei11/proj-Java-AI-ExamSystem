package com.hnust.examai.module.feedback;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hnust.examai.entity.QuestionFeedback;
import com.hnust.examai.module.feedback.dto.FeedbackVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 题目纠错反馈 Mapper
 */
@Mapper
public interface FeedbackMapper extends BaseMapper<QuestionFeedback> {

    /**
     * 分页查询用户自己的反馈列表（JOIN t_question 获取题目摘要）
     *
     * @param page   分页对象
     * @param userId 用户 ID
     * @return 分页结果
     */
    IPage<FeedbackVO> selectByUserId(Page<FeedbackVO> page, @Param("userId") Long userId);

    /**
     * 管理员分页查询全部反馈（可按状态过滤）
     *
     * @param page   分页对象
     * @param status 状态过滤（null 表示查全部）
     * @return 分页结果
     */
    IPage<FeedbackVO> selectAllForAdmin(Page<FeedbackVO> page, @Param("status") Integer status);
}
