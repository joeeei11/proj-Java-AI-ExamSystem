package com.hnust.examai.module.mock;

import com.hnust.examai.common.result.R;
import com.hnust.examai.module.mock.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 模考模式接口
 */
@Tag(name = "模考模式")
@RestController
@RequestMapping("/api/mock")
@RequiredArgsConstructor
public class MockController {

    private final MockService mockService;

    @Operation(summary = "开始模考")
    @PostMapping("/start")
    public R<Map<String, Long>> start(@AuthenticationPrincipal UserDetails user,
                                      @Valid @RequestBody MockStartRequest req) {
        Long userId = Long.parseLong(user.getUsername());
        Long examId = mockService.start(userId, req);
        return R.ok(Map.of("examId", examId));
    }

    @Operation(summary = "获取进行中模考（含题目，不含答案）")
    @GetMapping("/{examId}")
    public R<MockExamVO> getExam(@AuthenticationPrincipal UserDetails user,
                                 @PathVariable Long examId) {
        Long userId = Long.parseLong(user.getUsername());
        return R.ok(mockService.getExam(userId, examId));
    }

    @Operation(summary = "标记/取消标记题目")
    @PutMapping("/{examId}/flag")
    public R<Void> flag(@AuthenticationPrincipal UserDetails user,
                        @PathVariable Long examId,
                        @Valid @RequestBody FlagRequest req) {
        Long userId = Long.parseLong(user.getUsername());
        mockService.flag(userId, examId, req);
        return R.ok();
    }

    @Operation(summary = "交卷")
    @PostMapping("/{examId}/submit")
    public R<MockReportVO> submit(@AuthenticationPrincipal UserDetails user,
                                  @PathVariable Long examId,
                                  @Valid @RequestBody MockSubmitRequest req) {
        Long userId = Long.parseLong(user.getUsername());
        return R.ok(mockService.submit(userId, examId, req));
    }

    @Operation(summary = "获取模考报告")
    @GetMapping("/{examId}/report")
    public R<MockReportVO> getReport(@AuthenticationPrincipal UserDetails user,
                                     @PathVariable Long examId) {
        Long userId = Long.parseLong(user.getUsername());
        return R.ok(mockService.getReport(userId, examId));
    }

    @Operation(summary = "历史模考列表")
    @GetMapping("/history")
    public R<List<MockHistoryVO>> listHistory(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = Long.parseLong(user.getUsername());
        return R.ok(mockService.listHistory(userId, page, size));
    }
}
