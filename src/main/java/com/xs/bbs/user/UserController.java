package com.xs.bbs.user;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xs.bbs.common.ApiResponse;
import com.xs.bbs.social.RelationshipService;
import com.xs.bbs.social.UserHomeView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final RelationshipService relationshipService;

    public UserController(UserService userService, RelationshipService relationshipService) {
        this.userService = userService;
        this.relationshipService = relationshipService;
    }

    @GetMapping
    public ApiResponse<Collection<UserProfile>> listUsers() {
        return ApiResponse.ok(userService.listUsers());
    }

    @SaCheckLogin
    @GetMapping("/{userId}/home")
    public ApiResponse<UserHomeView> home(
            @PathVariable Long userId,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(relationshipService.getUserHome(StpUtil.getLoginIdAsLong(), userId, keyword));
    }
}
