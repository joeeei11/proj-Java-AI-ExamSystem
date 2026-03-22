package com.hnust.examai.module.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnust.examai.entity.StudyStat;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

/**
 * 学习统计 Mapper
 */
@Mapper
public interface StudyStatMapper extends BaseMapper<StudyStat> {

    /**
     * 标记今日打卡（UPSERT）
     */
    @Insert("INSERT INTO t_study_stat (user_id, stat_date, checkin) VALUES (#{userId}, #{date}, 1) " +
            "ON DUPLICATE KEY UPDATE checkin = 1")
    int upsertCheckin(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * 查询某日是否已打卡
     */
    @Select("SELECT COUNT(*) FROM t_study_stat WHERE user_id = #{userId} AND stat_date = #{date} AND checkin = 1")
    int countCheckin(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * 更新答题统计（UPSERT：已存在则累加 answer_count / correct_count）
     */
    @Insert("INSERT INTO t_study_stat (user_id, stat_date, answer_count, correct_count) " +
            "VALUES (#{userId}, #{date}, #{answerCount}, #{correctCount}) " +
            "ON DUPLICATE KEY UPDATE answer_count = answer_count + #{answerCount}, " +
            "correct_count = correct_count + #{correctCount}")
    int upsertAnswerStat(@Param("userId") Long userId, @Param("date") LocalDate date,
                         @Param("answerCount") int answerCount, @Param("correctCount") int correctCount);
}
