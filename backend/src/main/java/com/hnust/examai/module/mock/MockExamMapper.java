package com.hnust.examai.module.mock;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnust.examai.entity.MockExam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 模考记录 Mapper
 */
@Mapper
public interface MockExamMapper extends BaseMapper<MockExam> {

    /**
     * 批量超时处理：将 status=0 且到期的模考置为 status=2
     *
     * @param now 当前时间
     * @return 更新行数
     */
    @Update("UPDATE t_mock_exam SET status = 2 " +
            "WHERE status = 0 " +
            "AND DATE_ADD(started_at, INTERVAL time_limit_min MINUTE) < #{now}")
    int batchMarkTimeout(@Param("now") LocalDateTime now);

    /**
     * 查询用户历史模考（倒序分页，已完成/超时）
     */
    List<MockExam> selectHistoryByUserId(@Param("userId") Long userId,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);
}
