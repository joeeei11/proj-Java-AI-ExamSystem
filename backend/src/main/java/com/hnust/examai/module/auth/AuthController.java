package com.hnust.examai.module.auth;

import com.hnust.examai.common.result.R;
import com.hnust.examai.module.auth.dto.LoginRequest;
import com.hnust.examai.module.auth.dto.LoginResponse;
import com.hnust.examai.module.auth.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口：注册、登录、注销
 */
@Tag(name = "认证", description = "注册、登录、注销")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 注册
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return R.ok("注册成功", null);
    }

    /**
     * 登录
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return R.ok(response);
    }

    /**
     * 注销（将 token 加入黑名单）
     */
    @Operation(summary = "注销登录")
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            authService.logout(header.substring(7));
        }
        return R.ok("已退出登录", null);
    }
}
