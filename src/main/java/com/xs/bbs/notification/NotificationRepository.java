package com.xs.bbs.notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {

    NotificationMessage save(NotificationCreateCommand command);

    List<NotificationMessage> findByReceiverId(Long receiverId);

    Optional<NotificationMessage> findById(Long notificationId);

    long countUnread(Long receiverId);

    NotificationMessage markRead(Long notificationId);
}
