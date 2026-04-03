package com.xs.bbs.match;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xs.bbs.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/{userId}")
    public ApiResponse<List<MatchCandidateView>> getCandidates(
            @PathVariable Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserVisibilityLevel visibilityLevel
    ) {
        return ApiResponse.ok(matchService.getUserCandidates(userId, keyword, visibilityLevel));
    }

    @SaCheckLogin
    @GetMapping("/me")
    public ApiResponse<List<MatchCandidateView>> myCandidates(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserVisibilityLevel visibilityLevel
    ) {
        return ApiResponse.ok(matchService.getUserCandidates(StpUtil.getLoginIdAsLong(), keyword, visibilityLevel));
    }
}
