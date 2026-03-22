package com.hnust.examai.module.admin;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 管理员统计 Mapper（跨表 COUNT 查询）
 */
@Mapper
public interface AdminStatsMapper {

    /** 全平台答题总数 */
    @Select("SELECT COUNT(*) FROM t_user_answer")
    Long countTotalAnswers();

    /** 今日活跃用户数（today 有答题记录的去重用户数） */
    @Select("SELECT COUNT(DISTINCT user_id) FROM t_user_answer WHERE DATE(created_at) = CURDATE()")
    Long countTodayActiveUsers();
}
