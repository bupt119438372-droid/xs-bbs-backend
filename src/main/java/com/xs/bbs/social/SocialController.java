package com.xs.bbs.social;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xs.bbs.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/social")
public class SocialController {

    private final SocialGraphService socialGraphService;
    private final RelationshipService relationshipService;

    public SocialController(SocialGraphService socialGraphService, RelationshipService relationshipService) {
        this.socialGraphService = socialGraphService;
        this.relationshipService = relationshipService;
    }

    @SaCheckLogin
    @PostMapping("/follow")
    public ApiResponse<FollowStatusView> follow(@Valid @RequestBody FollowRequest request) {
        return ApiResponse.ok(socialGraphService.follow(StpUtil.getLoginIdAsLong(), request.targetId()));
    }

    @GetMapping("/status/{followerId}/{targetId}")
    public ApiResponse<FollowStatusView> status(@PathVariable Long followerId, @PathVariable Long targetId) {
        return ApiResponse.ok(socialGraphService.status(followerId, targetId));
    }

    @SaCheckLogin
    @GetMapping("/status/me/{targetId}")
    public ApiResponse<FollowStatusView> myStatus(@PathVariable Long targetId) {
        return ApiResponse.ok(socialGraphService.status(StpUtil.getLoginIdAsLong(), targetId));
    }

    @SaCheckLogin
    @GetMapping("/following/me")
    public ApiResponse<java.util.List<SocialUserCardView>> myFollowing(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(relationshipService.listFollowing(StpUtil.getLoginIdAsLong(), keyword));
    }

    @SaCheckLogin
    @GetMapping("/followers/me")
    public ApiResponse<java.util.List<SocialUserCardView>> myFollowers(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(relationshipService.listFollowers(StpUtil.getLoginIdAsLong(), keyword));
    }
}
