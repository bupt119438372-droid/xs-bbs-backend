package com.xs.bbs.notification;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.xs.bbs.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@SaCheckLogin
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/me")
    public ApiResponse<List<NotificationMessage>> listMine(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) NotificationType type
    ) {
        return ApiResponse.ok(notificationService.listByReceiver(StpUtil.getLoginIdAsLong(), keyword, type));
    }

    @GetMapping("/me/unread-count")
    public ApiResponse<Long> unreadCount() {
        return ApiResponse.ok(notificationService.unreadCount(StpUtil.getLoginIdAsLong()));
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<NotificationMessage> markRead(@PathVariable Long notificationId) {
        return ApiResponse.ok(notificationService.markRead(StpUtil.getLoginIdAsLong(), notificationId));
    }
}
