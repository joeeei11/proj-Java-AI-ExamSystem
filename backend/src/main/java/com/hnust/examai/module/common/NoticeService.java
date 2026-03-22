package com.hnust.examai.module.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 站内通知 Service（基于 Redis List）
 * <p>
 * TDR-007：通知用 Redis List 存储，key: notice:{userId}，TTL 7 天。
 * 不建数据库表，时效性数据，轻量实现。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService {

    private static final String KEY_PREFIX = "notice:";
    /** 通知存储时长：7 天 */
    private static final long TTL_DAYS = 7;
    /** 最多保留消息条数，防止无限增长 */
    private static final long MAX_NOTICES = 50;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 向指定用户推送站内通知
     *
     * @param userId  接收通知的用户 ID
     * @param message 通知内容
     */
    public void push(Long userId, String message) {
        String key = KEY_PREFIX + userId;
        String content = formatMessage(message);
        try {
            redisTemplate.opsForList().leftPush(key, content);
            // 修剪列表，只保留最近 MAX_NOTICES 条
            redisTemplate.opsForList().trim(key, 0, MAX_NOTICES - 1);
            // 刷新 TTL
            redisTemplate.expire(key, TTL_DAYS, TimeUnit.DAYS);
            log.info("站内通知已推送，userId={}, message={}", userId, message);
        } catch (Exception e) {
            // 通知失败不影响主流程
            log.error("站内通知推送失败，userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 获取用户的所有通知（最新在前）
     *
     * @param userId 用户 ID
     * @return 通知列表
     */
    public List<Object> getAll(Long userId) {
        String key = KEY_PREFIX + userId;
        try {
            List<Object> notices = redisTemplate.opsForList().range(key, 0, -1);
            return notices != null ? notices : List.of();
        } catch (Exception e) {
            log.error("获取站内通知失败，userId={}, error={}", userId, e.getMessage());
            return List.of();
        }
    }

    /**
     * 清空用户所有通知
     *
     * @param userId 用户 ID
     */
    public void clear(Long userId) {
        String key = KEY_PREFIX + userId;
        redisTemplate.delete(key);
    }

    // ===== 私有方法 =====

    private String formatMessage(String message) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
        return "[" + time + "] " + message;
    }
}
