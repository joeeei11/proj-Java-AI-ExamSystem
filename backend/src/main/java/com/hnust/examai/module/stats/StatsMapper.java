package com.hnust.examai.module.stats;

import com.hnust.examai.module.stats.dto.DailyStatVO;
import com.hnust.examai.module.stats.dto.StatsOverviewVO;
import com.hnust.examai.module.stats.dto.SubjectStatVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 统计模块 Mapper
 */
@Mapper
public interface StatsMapper {

    /**
     * 查询用户学习总览（累计答题/正确/天数）
     *
     * @param userId 用户 ID
     */
    StatsOverviewVO selectOverview(@Param("userId") Long userId);

    /**
     * 查询近 N 天日度答题统计
     *
     * @param userId 用户 ID
     * @param days   天数范围
     */
    List<DailyStatVO> selectDailyStats(@Param("userId") Long userId, @Param("days") int days);

    /**
     * 按科目聚合答题统计
     *
     * @param userId 用户 ID
     */
    List<SubjectStatVO> selectSubjectStats(@Param("userId") Long userId);

    /**
     * 查询错题总数
     *
     * @param userId 用户 ID
     */
    Long countTotalErrors(@Param("userId") Long userId);

    /**
     * 查询已掌握错题数
     *
     * @param userId 用户 ID
     */
    Long countMasteredErrors(@Param("userId") Long userId);
}
