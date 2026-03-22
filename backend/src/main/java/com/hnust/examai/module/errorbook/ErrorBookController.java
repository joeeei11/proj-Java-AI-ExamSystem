package com.hnust.examai.module.errorbook;

import com.hnust.examai.common.result.R;
import com.hnust.examai.module.errorbook.dto.ErrorBookVO;
import com.hnust.examai.module.errorbook.dto.ReviewRequest;
import com.hnust.examai.module.errorbook.dto.SetReasonRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 错题本接口
 */
@Tag(name = "错题本", description = "错题查询、错因标签、间隔复习")
@RestController
@RequestMapping("/api/errors")
@RequiredArgsConstructor
public class ErrorBookController {

    private final ErrorBookService errorBookService;

    /**
     * 错题列表（支持科目/掌握状态过滤）
     */
    @Operation(summary = "错题列表")
    @GetMapping
    public R<List<ErrorBookVO>> list(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Integer isMastered) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return R.ok(errorBookService.list(userId, subjectId, isMastered));
    }

    /**
     * 设置错因标签
     */
    @Operation(summary = "设置错因标签")
    @PutMapping("/{id}/reason")
    public R<Void> setReason(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody SetReasonRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        errorBookService.setReason(userId, id, request.getReason());
        return R.ok(null);
    }

    /**
     * 今日待复做列表（SRS 到期）
     */
    @Operation(summary = "今日待复做列表")
    @GetMapping("/today-review")
    public R<List<ErrorBookVO>> todayReview(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return R.ok(errorBookService.todayReview(userId));
    }

    /**
     * 提交复做结果（更新 SRS 调度字段）
     */
    @Operation(summary = "提交复做结果")
    @PostMapping("/{id}/review")
    public R<Void> submitReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        errorBookService.submitReview(userId, id, request.getKnown());
        return R.ok(null);
    }
}
