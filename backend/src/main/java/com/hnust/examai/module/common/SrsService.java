package com.hnust.examai.module.common;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 间隔复习（SRS）算法服务
 * <p>
 * 无状态工具类，错题本和抽认卡共用同一套算法：<br>
 * - 用户点"会了"：nextInterval = min(ceil(currentInterval × 2.5), 60)<br>
 * - 用户点"不会"：nextInterval = 1（重置）
 * </p>
 */
@Service
public class SrsService {

    /** 间隔增长倍数 */
    private static final double INTERVAL_MULTIPLIER = 2.5;

    /** 最大间隔天数 */
    private static final int MAX_INTERVAL_DAYS = 60;

    /**
     * 根据复做结果计算新的间隔天数
     *
     * @param known           用户是否已掌握
     * @param currentInterval 当前间隔天数（首次复习传 1）
     * @return 新的间隔天数
     */
    public int calcNextInterval(boolean known, int currentInterval) {
        if (known) {
            return Math.min((int) Math.ceil(currentInterval * INTERVAL_MULTIPLIER), MAX_INTERVAL_DAYS);
        }
        return 1;
    }

    /**
     * 根据间隔天数计算下次复习时间
     *
     * @param intervalDays 间隔天数
     * @return 下次复习时间
     */
    public LocalDateTime calcNextReviewAt(int intervalDays) {
        return LocalDateTime.now().plusDays(intervalDays);
    }
}
