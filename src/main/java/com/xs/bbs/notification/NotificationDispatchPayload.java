package com.xs.bbs.notification;

import java.time.LocalDateTime;

public record NotificationDispatchPayload(
        Long notificationId,
        Long receiverId,
        Long senderId,
        NotificationType type,
        String title,
        String content,
        LocalDateTime createdAt
) {
}
