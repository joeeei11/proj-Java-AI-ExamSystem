package com.hnust.examai.module.feedback;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hnust.examai.common.result.R;
import com.hnust.examai.module.feedback.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 题目纠错反馈 Controller
 * <p>
 * 用户端：/api/feedback/**<br>
 * 管理员端：/api/admin/feedback/**（需 ADMIN 角色，由 SecurityConfig 全局拦截）
 * </p>
 */
@Tag(name = "题目纠错反馈")
@RestController
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    private Long getUserId(UserDetails userDetails) {
        return Long.parseLong(userDetails.getUsername());
    }

    // ===== 用户端接口 =====

    @Operation(summary = "提交纠错反馈")
    @PostMapping("/api/feedback/question")
    public R<Void> submit(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SubmitFeedbackRequest request) {
        feedbackService.submit(getUserId(userDetails), request);
        return R.ok();
    }

    @Operation(summary = "我的反馈列表")
    @GetMapping("/api/feedback/my")
    public R<IPage<FeedbackVO>> listMy(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return R.ok(feedbackService.listMy(getUserId(userDetails), page, size));
    }

    @Operation(summary = "我的反馈详情")
    @GetMapping("/api/feedback/my/{id}")
    public R<FeedbackDetailVO> getMy(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return R.ok(feedbackService.getMy(getUserId(userDetails), id));
    }

    // ===== 管理员端接口 =====

    @Operation(summary = "管理员：查看全部反馈")
    @GetMapping("/api/admin/feedback")
    public R<IPage<FeedbackVO>> listAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        return R.ok(feedbackService.listAll(page, size, status));
    }

    @Operation(summary = "管理员：处理反馈")
    @PutMapping("/api/admin/feedback/{id}")
    public R<Void> handle(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody HandleFeedbackRequest request) {
        feedbackService.handle(getUserId(userDetails), id, request);
        return R.ok();
    }
}
