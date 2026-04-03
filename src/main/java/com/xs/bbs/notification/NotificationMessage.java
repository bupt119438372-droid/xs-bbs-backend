package com.xs.bbs.notification;

import java.time.LocalDateTime;

public record NotificationMessage(
        Long id,
        Long receiverId,
        Long senderId,
        Long relatedThoughtId,
        NotificationType type,
        String title,
        String content,
        NotificationStatus status,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
}
