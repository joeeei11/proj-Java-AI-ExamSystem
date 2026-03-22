package com.hnust.examai.module.user;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hnust.examai.common.exception.BizException;
import com.hnust.examai.common.result.ResultCode;
import com.hnust.examai.common.utils.MinioUtil;
import com.hnust.examai.entity.User;
import com.hnust.examai.module.auth.UserMapper;
import com.hnust.examai.module.user.dto.ChangePasswordRequest;
import com.hnust.examai.module.user.dto.ProfileResponse;
import com.hnust.examai.module.user.dto.UpdateProfileRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 用户 Service：个人信息、头像、打卡
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String CHECKIN_KEY_PREFIX = "checkin:";
    private static final String CHECKIN_LOCK_PREFIX = "lock:checkin:";
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024L; // 2MB

    private final UserMapper userMapper;
    private final StudyStatMapper studyStatMapper;
    private final PasswordEncoder passwordEncoder;
    private final MinioUtil minioUtil;
    private final StringRedisTemplate redisTemplate;

    /**
     * 获取用户个人信息
     */
    public ProfileResponse getProfile(Long userId) {
        User user = getUser(userId);
        return toProfileResponse(user);
    }

    /**
     * 更新昵称/邮箱
     */
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId);

        if (request.getNickname() != null) {
            wrapper.set(User::getNickname, request.getNickname());
        }
        if (request.getEmail() != null) {
            wrapper.set(User::getEmail, request.getEmail());
        }

        userMapper.update(null, wrapper);
        return toProfileResponse(getUser(userId));
    }

    /**
     * 修改密码
     */
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getUser(userId);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BizException(ResultCode.PASSWORD_WRONG);
        }

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPassword, passwordEncoder.encode(request.getNewPassword())));

        log.info("用户 {} 修改密码成功", userId);
    }

    /**
     * 上传头像（限 2MB，仅支持图片）
     */
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "文件不能为空");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BizException(ResultCode.FILE_SIZE_EXCEEDED);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BizException(ResultCode.FILE_TYPE_NOT_ALLOWED);
        }

        // 删除旧头像（如存在）
        User user = getUser(userId);
        if (user.getAvatarUrl() != null) {
            String oldObject = minioUtil.extractObjectName(user.getAvatarUrl());
            if (oldObject != null) {
                minioUtil.delete(oldObject);
            }
        }

        String url = minioUtil.upload(file, "avatars/");
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getAvatarUrl, url));

        return url;
    }

    /**
     * 每日打卡（Redis 分布式锁防并发重复打卡）
     *
     * @return 当前连续打卡天数
     */
    public int checkIn(Long userId) {
        LocalDate today = LocalDate.now();
        String lockKey = CHECKIN_LOCK_PREFIX + userId;
        String checkinKey = CHECKIN_KEY_PREFIX + userId + ":" + today.format(DateTimeFormatter.BASIC_ISO_DATE);

        // 尝试获取锁（30秒超时）
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 30, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BizException("操作太频繁，请稍后重试");
        }

        try {
            // 检查今日是否已打卡
            if (Boolean.TRUE.equals(redisTemplate.hasKey(checkinKey))) {
                throw new BizException(ResultCode.CHECKIN_DUPLICATE);
            }

            // 标记今日已打卡（到明天 0 点过期）
            redisTemplate.opsForValue().set(checkinKey, "1", 86400, TimeUnit.SECONDS);

            // 写入学习统计
            studyStatMapper.upsertCheckin(userId, today);

            // 计算连续打卡天数
            int streak = calculateStreak(userId, today);
            userMapper.updateStudyStreak(userId, streak);

            log.info("用户 {} 打卡成功，连续 {} 天", userId, streak);
            return streak;

        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    // ===== 私有方法 =====

    private User getUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * 计算连续打卡天数（从今天往前推）
     */
    private int calculateStreak(Long userId, LocalDate today) {
        int streak = 1; // 今天已打卡
        LocalDate check = today.minusDays(1);
        while (studyStatMapper.countCheckin(userId, check) > 0) {
            streak++;
            check = check.minusDays(1);
            if (streak > 365) break; // 防止无限循环
        }
        return streak;
    }

    private ProfileResponse toProfileResponse(User user) {
        ProfileResponse res = new ProfileResponse();
        res.setId(user.getId());
        res.setUsername(user.getUsername());
        res.setNickname(user.getNickname());
        res.setEmail(user.getEmail());
        res.setAvatarUrl(user.getAvatarUrl());
        res.setRole(user.getRole());
        res.setStudyStreak(user.getStudyStreak() != null ? user.getStudyStreak() : 0);
        if (user.getLastLoginAt() != null) {
            res.setLastLoginAt(user.getLastLoginAt().toString());
        }
        if (user.getCreatedAt() != null) {
            res.setCreatedAt(user.getCreatedAt().toString());
        }
        return res;
    }
}
