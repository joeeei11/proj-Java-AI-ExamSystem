package com.hnust.examai.module.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnust.examai.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 更新最后登录时间
     */
    @Update("UPDATE t_user SET last_login_at = NOW() WHERE id = #{userId}")
    int updateLastLoginAt(@Param("userId") Long userId);

    /**
     * 更新连续打卡天数
     */
    @Update("UPDATE t_user SET study_streak = #{streak} WHERE id = #{userId}")
    int updateStudyStreak(@Param("userId") Long userId, @Param("streak") int streak);
}
