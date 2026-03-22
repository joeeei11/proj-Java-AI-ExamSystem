package com.hnust.examai.common.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 * 使用 HS256 算法，密钥长度至少 32 字节
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expire-hours}")
    private long expireHours;

    /**
     * 生成 JWT Token
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @param role     角色（0:用户 1:管理员）
     * @return JWT 字符串
     */
    public String generateToken(Long userId, String username, Integer role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expireHours * 3600_000L);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * 解析 Token 获取 Claims
     *
     * @param token JWT 字符串
     * @return Claims 对象
     * @throws JwtException token 无效或过期时抛出
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Claims 中获取用户 ID
     */
    public Long getUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    /**
     * 从 Claims 中获取用户名
     */
    public String getUsername(Claims claims) {
        return claims.get("username", String.class);
    }

    /**
     * 从 Claims 中获取角色
     */
    public Integer getRole(Claims claims) {
        return claims.get("role", Integer.class);
    }

    /**
     * 获取 token 剩余有效毫秒数（用于设置黑名单 TTL）
     */
    public long getRemainingMillis(Claims claims) {
        return claims.getExpiration().getTime() - System.currentTimeMillis();
    }

    /**
     * 校验 token 是否有效（不抛异常）
     */
    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
