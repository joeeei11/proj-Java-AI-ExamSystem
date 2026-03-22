package com.hnust.examai.module.user;

import com.hnust.examai.common.result.R;
import com.hnust.examai.module.user.dto.ChangePasswordRequest;
import com.hnust.examai.module.user.dto.ProfileResponse;
import com.hnust.examai.module.user.dto.UpdateProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 用户接口：个人资料、头像、打卡
 */
@Tag(name = "用户", description = "个人资料、头像、打卡")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer")
public class UserController {

    private final UserService userService;

    private Long getUserId(UserDetails userDetails) {
        return Long.parseLong(userDetails.getUsername());
    }

    @Operation(summary = "获取个人资料")
    @GetMapping("/user/profile")
    public R<ProfileResponse> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return R.ok(userService.getProfile(getUserId(userDetails)));
    }

    @Operation(summary = "更新个人资料")
    @PutMapping("/user/profile")
    public R<ProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        return R.ok(userService.updateProfile(getUserId(userDetails), request));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/auth/password")
    public R<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(getUserId(userDetails), request);
        return R.ok("密码修改成功", null);
    }

    @Operation(summary = "上传头像")
    @PostMapping("/user/avatar")
    public R<Map<String, String>> uploadAvatar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        String url = userService.uploadAvatar(getUserId(userDetails), file);
        return R.ok(Map.of("avatarUrl", url));
    }

    @Operation(summary = "每日打卡")
    @PostMapping("/user/check-in")
    public R<Map<String, Integer>> checkIn(@AuthenticationPrincipal UserDetails userDetails) {
        int streak = userService.checkIn(getUserId(userDetails));
        return R.ok(Map.of("studyStreak", streak));
    }
}
