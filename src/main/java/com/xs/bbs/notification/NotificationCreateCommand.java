package com.xs.bbs.notification;

public record NotificationCreateCommand(
        Long receiverId,
        Long senderId,
        Long relatedThoughtId,
        NotificationType type,
        String title,
        String content
) {
}
