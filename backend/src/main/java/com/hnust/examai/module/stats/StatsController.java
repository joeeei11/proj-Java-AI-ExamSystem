package com.hnust.examai.module.stats;

import com.hnust.examai.common.result.R;
import com.hnust.examai.module.stats.dto.DailyStatVO;
import com.hnust.examai.module.stats.dto.StatsOverviewVO;
import com.hnust.examai.module.stats.dto.SubjectStatVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学习统计接口
 */
@Tag(name = "学习统计", description = "总览数据、日度趋势、科目分布")
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /**
     * 学习总览数据
     */
    @Operation(summary = "学习总览")
    @GetMapping("/overview")
    public R<StatsOverviewVO> overview(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return R.ok(statsService.getOverview(userId));
    }

    /**
     * 近 N 天日度统计（默认30天，无答题日期自动补零）
     */
    @Operation(summary = "日度答题趋势")
    @GetMapping("/daily")
    public R<List<DailyStatVO>> daily(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "30") int days) {
        if (days < 1 || days > 365) {
            days = 30;
        }
        Long userId = Long.parseLong(userDetails.getUsername());
        return R.ok(statsService.getDailyStats(userId, days));
    }

    /**
     * 科目维度答题统计
     */
    @Operation(summary = "科目统计")
    @GetMapping("/subjects")
    public R<List<SubjectStatVO>> subjects(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return R.ok(statsService.getSubjectStats(userId));
    }
}
