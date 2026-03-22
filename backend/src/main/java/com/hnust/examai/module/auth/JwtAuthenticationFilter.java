package com.hnust.examai.module.auth;

import com.hnust.examai.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器
 * 每次请求从 Authorization Header 中提取并验证 JWT Token
 * 有效则将用户信息注入 Spring Security Context
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final AuthService authService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token)) {
            try {
                // 1. 解析 token
                Claims claims = jwtUtil.parseToken(token);

                // 2. 检查黑名单（已注销的 token）
                if (!authService.isBlacklisted(token)) {
                    Long userId = jwtUtil.getUserId(claims);
                    Integer role = jwtUtil.getRole(claims);

                    // 3. 构建权限列表
                    String roleStr = (role != null && role == 1) ? "ROLE_ADMIN" : "ROLE_USER";
                    List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(roleStr));

                    // 4. 设置 SecurityContext，principal 为 UserDetails（username 存 userId）
                    var userDetails = new org.springframework.security.core.userdetails.User(
                            userId.toString(), "", authorities);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException | IllegalArgumentException e) {
                // token 无效或过期，不设置认证，后续 Spring Security 会返回 401
                log.debug("JWT 解析失败: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从 Authorization 头提取 Bearer token
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
