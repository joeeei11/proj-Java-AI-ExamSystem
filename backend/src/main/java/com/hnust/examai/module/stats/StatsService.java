package com.hnust.examai.module.stats;

import com.hnust.examai.module.stats.dto.DailyStatVO;
import com.hnust.examai.module.stats.dto.StatsOverviewVO;
import com.hnust.examai.module.stats.dto.SubjectStatVO;
import com.hnust.examai.module.auth.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学习统计 Service
 * <p>
 * 统计数据从 t_study_stat 汇总表读取（不实时聚合 t_user_answer），
 * 科目统计从 t_user_answer JOIN t_question 聚合。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final StatsMapper statsMapper;
    private final UserMapper  userMapper;

    /**
     * 获取学习总览数据
     *
     * @param userId 用户 ID
     */
    public StatsOverviewVO getOverview(Long userId) {
        StatsOverviewVO vo = statsMapper.selectOverview(userId);
        if (vo == null) {
            vo = new StatsOverviewVO();
            vo.setTotalAnswered(0L);
            vo.setTotalCorrect(0L);
            vo.setStudyDays(0L);
        }

        // 正确率
        vo.setAccuracy(calcAccuracy(vo.getTotalAnswered(), vo.getTotalCorrect()));

        // 连续打卡天数从用户表获取
        var user = userMapper.selectById(userId);
        vo.setStudyStreak(user != null ? user.getStudyStreak() : 0);

        // 错题统计
        vo.setTotalErrors(statsMapper.countTotalErrors(userId));
        vo.setMasteredErrors(statsMapper.countMasteredErrors(userId));

        return vo;
    }

    /**
     * 获取近 N 天日度统计（无答题日期自动补零，保证折线图连续）
     *
     * @param userId 用户 ID
     * @param days   天数（如 30）
     */
    public List<DailyStatVO> getDailyStats(Long userId, int days) {
        List<DailyStatVO> dbRows = statsMapper.selectDailyStats(userId, days);

        // 构建日期 → 数据 Map，方便补零查找
        Map<String, DailyStatVO> dataMap = dbRows.stream()
                .collect(Collectors.toMap(DailyStatVO::getDate, v -> v));

        // 生成近 days 天完整日期序列，无数据日期补零
        List<DailyStatVO> result = new ArrayList<>(days);
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            String dateStr = today.minusDays(i).format(DATE_FMT);
            DailyStatVO row = dataMap.getOrDefault(dateStr, new DailyStatVO(dateStr, 0, 0, 0.0));
            row.setAccuracy(calcAccuracy(row.getTotalAnswered().longValue(),
                    row.getCorrectCount().longValue()));
            result.add(row);
        }
        return result;
    }

    /**
     * 获取科目维度答题统计
     *
     * @param userId 用户 ID
     */
    public List<SubjectStatVO> getSubjectStats(Long userId) {
        List<SubjectStatVO> list = statsMapper.selectSubjectStats(userId);
        list.forEach(vo -> vo.setAccuracy(calcAccuracy(vo.getTotalAnswered(), vo.getCorrectCount())));
        return list;
    }

    // ===== 私有方法 =====

    /**
     * 计算正确率，保留1位小数；分母为0时返回0.0
     */
    private static double calcAccuracy(long total, long correct) {
        if (total == 0) {
            return 0.0;
        }
        return Math.round((double) correct / total * 1000) / 10.0;
    }
}
