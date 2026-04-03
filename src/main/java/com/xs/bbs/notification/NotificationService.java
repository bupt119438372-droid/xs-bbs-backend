package com.xs.bbs.notification;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MessagePublisher messagePublisher;

    public NotificationService(NotificationRepository notificationRepository, MessagePublisher messagePublisher) {
        this.notificationRepository = notificationRepository;
        this.messagePublisher = messagePublisher;
    }

    @Transactional
    @CacheEvict(cacheNames = "notificationUnreadCount", key = "#command.receiverId")
    public NotificationMessage create(NotificationCreateCommand command) {
        NotificationMessage message = notificationRepository.save(command);
        messagePublisher.publish(new NotificationDispatchPayload(
                message.id(),
                message.receiverId(),
                message.senderId(),
                message.type(),
                message.title(),
                message.content(),
                message.createdAt()
        ));
        return message;
    }

    public List<NotificationMessage> listByReceiver(Long receiverId) {
        return notificationRepository.findByReceiverId(receiverId);
    }

    public List<NotificationMessage> listByReceiver(Long receiverId, String keyword, NotificationType type) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return notificationRepository.findByReceiverId(receiverId).stream()
                .filter(message -> type == null || message.type() == type)
                .filter(message -> normalizedKeyword.isBlank()
                        || contains(message.title(), normalizedKeyword)
                        || contains(message.content(), normalizedKeyword))
                .toList();
    }

    @Cacheable(cacheNames = "notificationUnreadCount", key = "#receiverId")
    public long unreadCount(Long receiverId) {
        return notificationRepository.countUnread(receiverId);
    }

    @Transactional
    @CacheEvict(cacheNames = "notificationUnreadCount", key = "#receiverId")
    public NotificationMessage markRead(Long receiverId, Long notificationId) {
        NotificationMessage message = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("notification does not exist"));
        if (!message.receiverId().equals(receiverId)) {
            throw new IllegalArgumentException("notification does not belong to current user");
        }
        return notificationRepository.markRead(notificationId);
    }

    private boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
