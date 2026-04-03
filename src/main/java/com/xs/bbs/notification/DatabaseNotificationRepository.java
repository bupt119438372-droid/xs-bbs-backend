package com.xs.bbs.notification;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class DatabaseNotificationRepository implements NotificationRepository {

    private final NotificationMessageMapper notificationMessageMapper;

    public DatabaseNotificationRepository(NotificationMessageMapper notificationMessageMapper) {
        this.notificationMessageMapper = notificationMessageMapper;
    }

    @Override
    public NotificationMessage save(NotificationCreateCommand command) {
        NotificationMessageEntity entity = new NotificationMessageEntity();
        NotificationType type = command.type() == null ? NotificationType.SYSTEM : command.type();
        entity.setReceiverId(command.receiverId());
        entity.setSenderId(command.senderId());
        entity.setRelatedThoughtId(command.relatedThoughtId());
        entity.setTypeCode(type.name());
        entity.setTitle(command.title());
        entity.setContent(command.content());
        entity.setReadFlag(Boolean.FALSE);
        entity.setCreatedAt(LocalDateTime.now());
        notificationMessageMapper.insert(entity);
        return toDomain(entity);
    }

    @Override
    public List<NotificationMessage> findByReceiverId(Long receiverId) {
        return notificationMessageMapper.selectList(
                        Wrappers.lambdaQuery(NotificationMessageEntity.class)
                                .eq(NotificationMessageEntity::getReceiverId, receiverId)
                                .orderByDesc(NotificationMessageEntity::getCreatedAt)
                ).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<NotificationMessage> findById(Long notificationId) {
        return Optional.ofNullable(notificationMessageMapper.selectById(notificationId))
                .map(this::toDomain);
    }

    @Override
    public long countUnread(Long receiverId) {
        Long count = notificationMessageMapper.selectCount(
                Wrappers.lambdaQuery(NotificationMessageEntity.class)
                        .eq(NotificationMessageEntity::getReceiverId, receiverId)
                        .eq(NotificationMessageEntity::getReadFlag, Boolean.FALSE)
        );
        return count == null ? 0L : count;
    }

    @Override
    public NotificationMessage markRead(Long notificationId) {
        NotificationMessageEntity entity = notificationMessageMapper.selectById(notificationId);
        if (entity == null) {
            throw new IllegalArgumentException("notification does not exist");
        }
        if (Boolean.FALSE.equals(entity.getReadFlag())) {
            entity.setReadFlag(Boolean.TRUE);
            entity.setReadAt(LocalDateTime.now());
            notificationMessageMapper.updateById(entity);
        }
        return toDomain(entity);
    }

    private NotificationMessage toDomain(NotificationMessageEntity entity) {
        return new NotificationMessage(
                entity.getId(),
                entity.getReceiverId(),
                entity.getSenderId(),
                entity.getRelatedThoughtId(),
                parseType(entity.getTypeCode()),
                entity.getTitle(),
                entity.getContent(),
                Boolean.TRUE.equals(entity.getReadFlag()) ? NotificationStatus.READ : NotificationStatus.UNREAD,
                entity.getCreatedAt(),
                entity.getReadAt()
        );
    }

    private NotificationType parseType(String typeCode) {
        if (typeCode == null || typeCode.isBlank()) {
            return NotificationType.SYSTEM;
        }
        return NotificationType.valueOf(typeCode);
    }
}
