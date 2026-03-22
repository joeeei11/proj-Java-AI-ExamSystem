package com.hnust.examai.module.health;

import com.hnust.examai.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 健康检查接口
 */
@Tag(name = "系统", description = "健康检查")
@RestController
@RequestMapping("/api")
public class HealthController {

    @Operation(summary = "健康检查", description = "检查服务是否正常运行")
    @GetMapping("/health")
    public R<Map<String, String>> health() {
        return R.ok(Map.of(
                "status", "UP",
                "service", "hnust-exam-ai",
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        ));
    }
}
