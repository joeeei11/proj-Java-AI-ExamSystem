package com.hnust.examai.module.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hnust.examai.common.exception.BizException;
import com.hnust.examai.common.result.ResultCode;
import com.hnust.examai.common.utils.JwtUtil;
import com.hnust.examai.entity.User;
import com.hnust.examai.module.auth.dto.LoginRequest;
import com.hnust.examai.module.auth.dto.LoginResponse;
import com.hnust.examai.module.auth.dto.RegisterRequest;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 认证 Service：注册、登录、注销
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BLACKLIST_PREFIX = "blacklist:token:";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    /**
     * 用户注册
     *
     * @param request 注册信息
     */
    public void register(RegisterRequest request) {
        // 校验用户名唯一
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BizException(ResultCode.USERNAME_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setRole(0);
        user.setIsActive(1);
        user.setStudyStreak(0);

        userMapper.insert(user);
        log.info("新用户注册成功: {}", request.getUsername());
    }

    /**
     * 用户登录
     *
     * @param request 登录信息
     * @return 登录响应（token + 用户信息）
     */
    public LoginResponse login(LoginRequest request) {
        // 查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));

        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        if (user.getIsActive() == 0) {
            throw new BizException(ResultCode.USER_DISABLED);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(ResultCode.PASSWORD_WRONG);
        }

        // 更新最后登录时间
        userMapper.updateLastLoginAt(user.getId());

        // 生成 Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 构建响应
        LoginResponse response = new LoginResponse();
        response.setToken(token);

        LoginResponse.UserInfoVO userInfoVO = new LoginResponse.UserInfoVO();
        userInfoVO.setId(user.getId());
        userInfoVO.setUsername(user.getUsername());
        userInfoVO.setNickname(user.getNickname());
        userInfoVO.setAvatarUrl(user.getAvatarUrl());
        userInfoVO.setRole(user.getRole());
        userInfoVO.setStudyStreak(user.getStudyStreak() != null ? user.getStudyStreak() : 0);
        response.setUserInfo(userInfoVO);

        log.info("用户登录成功: {}", user.getUsername());
        return response;
    }

    /**
     * 注销：将 token 加入 Redis 黑名单
     *
     * @param token JWT token
     */
    public void logout(String token) {
        try {
            Claims claims = jwtUtil.parseToken(token);
            long ttl = jwtUtil.getRemainingMillis(claims);
            if (ttl > 0) {
                redisTemplate.opsForValue().set(
                        BLACKLIST_PREFIX + token, "1", ttl, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            // token 已过期或无效，无需处理
            log.debug("注销 token 处理: {}", e.getMessage());
        }
    }

    /**
     * 检查 token 是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}
