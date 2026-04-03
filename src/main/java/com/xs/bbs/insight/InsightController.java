package com.xs.bbs.insight;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xs.bbs.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/insights")
public class InsightController {

    private final InsightService insightService;

    public InsightController(InsightService insightService) {
        this.insightService = insightService;
    }

    @GetMapping("/daily/{userId}")
    public ApiResponse<DailyInsightView> daily(@PathVariable Long userId) {
        return ApiResponse.ok(insightService.buildDailyInsight(userId));
    }

    @SaCheckLogin
    @GetMapping("/daily/me")
    public ApiResponse<DailyInsightView> myDaily() {
        return ApiResponse.ok(insightService.buildDailyInsight(StpUtil.getLoginIdAsLong()));
    }
}
