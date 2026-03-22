package com.hnust.examai.module.review;

import com.hnust.examai.common.result.R;
import com.hnust.examai.module.review.dto.GenerateOutlineRequest;
import com.hnust.examai.module.review.dto.ReviewOutlineVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 复习中心接口
 */
@Tag(name = "复习中心", description = "AI 生成复习提纲、提纲管理")
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 生成复习提纲（基于所选笔记调用 AI）
     */
    @Operation(summary = "生成复习提纲")
    @PostMapping("/outline")
    public R<ReviewOutlineVO> generateOutline(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody GenerateOutlineRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return R.ok(reviewService.generateOutline(userId, request));
    }

    /**
     * 提纲列表（按创建时间倒序）
     */
    @Operation(summary = "提纲列表")
    @GetMapping("/outlines")
    public R<List<ReviewOutlineVO>> listOutlines(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return R.ok(reviewService.listOutlines(userId));
    }

    /**
     * 删除提纲
     */
    @Operation(summary = "删除提纲")
    @DeleteMapping("/outlines/{id}")
    public R<Void> deleteOutline(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = Long.parseLong(userDetails.getUsername());
        reviewService.deleteOutline(userId, id);
        return R.ok(null);
    }
}
