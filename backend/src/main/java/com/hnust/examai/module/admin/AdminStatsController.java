package com.hnust.examai.module.admin;

import com.hnust.examai.common.result.R;
import com.hnust.examai.module.admin.dto.SystemStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员 - 系统统计接口
 */
@Tag(name = "管理员 - 系统统计", description = "系统级数据总览")
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    /**
     * 获取系统级统计总览
     */
    @Operation(summary = "获取系统统计总览")
    @GetMapping
    public R<SystemStatsVO> getSystemStats() {
        return R.ok(adminStatsService.getSystemStats());
    }
}
