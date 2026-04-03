package com.xs.bbs.outbox;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xs.bbs.config.OutboxProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OutboxEventService {

    private final OutboxEventMapper outboxEventMapper;
    private final OutboxProperties outboxProperties;

    public OutboxEventService(OutboxEventMapper outboxEventMapper, OutboxProperties outboxProperties) {
        this.outboxEventMapper = outboxEventMapper;
        this.outboxProperties = outboxProperties;
    }

    public List<OutboxEventEntity> findPendingBatch() {
        LocalDateTime now = LocalDateTime.now();
        return outboxEventMapper.selectList(
                Wrappers.lambdaQuery(OutboxEventEntity.class)
                        .in(
                                OutboxEventEntity::getStatus,
                                OutboxEventStatus.PENDING.name(),
                                OutboxEventStatus.FAILED.name(),
                                OutboxEventStatus.PUBLISHED.name()
                        )
                        .le(OutboxEventEntity::getNextRetryAt, now)
                        .orderByAsc(OutboxEventEntity::getCreatedAt)
                        .last("limit " + outboxProperties.getBatchSize())
        );
    }

    public OutboxEventEntity findById(Long eventId) {
        return outboxEventMapper.selectById(eventId);
    }

    @Transactional
    public boolean markProcessing(Long eventId) {
        LocalDateTime now = LocalDateTime.now();
        OutboxEventEntity update = new OutboxEventEntity();
        update.setId(eventId);
        update.setStatus(OutboxEventStatus.PROCESSING.name());
        int affected = outboxEventMapper.update(
                update,
                Wrappers.lambdaUpdate(OutboxEventEntity.class)
                        .eq(OutboxEventEntity::getId, eventId)
                        .in(
                                OutboxEventEntity::getStatus,
                                OutboxEventStatus.PENDING.name(),
                                OutboxEventStatus.FAILED.name(),
                                OutboxEventStatus.PUBLISHED.name()
                        )
                        .le(OutboxEventEntity::getNextRetryAt, now)
        );
        return affected > 0;
    }

    @Transactional
    public boolean markConsumingPublished(Long eventId) {
        OutboxEventEntity update = new OutboxEventEntity();
        update.setId(eventId);
        update.setStatus(OutboxEventStatus.PROCESSING.name());
        int affected = outboxEventMapper.update(
                update,
                Wrappers.lambdaUpdate(OutboxEventEntity.class)
                        .eq(OutboxEventEntity::getId, eventId)
                        .eq(OutboxEventEntity::getStatus, OutboxEventStatus.PUBLISHED.name())
        );
        return affected > 0;
    }

    @Transactional
    public void markProcessed(Long eventId, String resultSummary) {
        outboxEventMapper.update(
                null,
                Wrappers.lambdaUpdate(OutboxEventEntity.class)
                        .eq(OutboxEventEntity::getId, eventId)
                        .set(OutboxEventEntity::getStatus, OutboxEventStatus.PROCESSED.name())
                        .set(OutboxEventEntity::getResultSummary, resultSummary)
                        .set(OutboxEventEntity::getLastError, null)
                        .set(OutboxEventEntity::getNextRetryAt, null)
                        .set(OutboxEventEntity::getProcessedAt, LocalDateTime.now())
                        .set(OutboxEventEntity::getDeadLetterReason, null)
                        .set(OutboxEventEntity::getDeadLetteredAt, null)
        );
    }

    @Transactional
    public void markPublished(Long eventId, String resultSummary) {
        outboxEventMapper.update(
                null,
                Wrappers.lambdaUpdate(OutboxEventEntity.class)
                        .eq(OutboxEventEntity::getId, eventId)
                        .set(OutboxEventEntity::getStatus, OutboxEventStatus.PUBLISHED.name())
                        .set(OutboxEventEntity::getResultSummary, resultSummary)
                        .set(OutboxEventEntity::getLastError, null)
                        .set(
                                OutboxEventEntity::getNextRetryAt,
                                LocalDateTime.now().plusNanos(outboxProperties.getPublishTimeoutMs() * 1_000_000)
                        )
                        .set(OutboxEventEntity::getDeadLetterReason, null)
                        .set(OutboxEventEntity::getDeadLetteredAt, null)
        );
    }

    @Transactional
    public OutboxEventStatus recordFailure(Long eventId, Exception exception) {
        OutboxEventEntity event = outboxEventMapper.selectById(eventId);
        if (event == null) {
            return null;
        }
        int nextRetryCount = (event.getRetryCount() == null ? 0 : event.getRetryCount()) + 1;
        OutboxEventEntity update = new OutboxEventEntity();
        update.setId(eventId);
        update.setRetryCount(nextRetryCount);
        update.setLastError(truncate(exception.getMessage(), 500));
        update.setProcessedAt(null);
        if (nextRetryCount >= outboxProperties.getMaxRetryCount()) {
            outboxEventMapper.update(
                    null,
                    Wrappers.lambdaUpdate(OutboxEventEntity.class)
                            .eq(OutboxEventEntity::getId, eventId)
                            .set(OutboxEventEntity::getRetryCount, nextRetryCount)
                            .set(OutboxEventEntity::getLastError, truncate(exception.getMessage(), 500))
                            .set(OutboxEventEntity::getProcessedAt, null)
                            .set(OutboxEventEntity::getStatus, OutboxEventStatus.DEAD.name())
                            .set(OutboxEventEntity::getDeadLetterReason, truncate(exception.getMessage(), 500))
                            .set(OutboxEventEntity::getDeadLetteredAt, LocalDateTime.now())
                            .set(OutboxEventEntity::getNextRetryAt, null)
            );
            return OutboxEventStatus.DEAD;
        } else {
            outboxEventMapper.update(
                    null,
                    Wrappers.lambdaUpdate(OutboxEventEntity.class)
                            .eq(OutboxEventEntity::getId, eventId)
                            .set(OutboxEventEntity::getRetryCount, nextRetryCount)
                            .set(OutboxEventEntity::getLastError, truncate(exception.getMessage(), 500))
                            .set(OutboxEventEntity::getProcessedAt, null)
                            .set(OutboxEventEntity::getStatus, OutboxEventStatus.FAILED.name())
                            .set(OutboxEventEntity::getDeadLetterReason, null)
                            .set(OutboxEventEntity::getDeadLetteredAt, null)
                            .set(
                                    OutboxEventEntity::getNextRetryAt,
                                    LocalDateTime.now().plusNanos(computeBackoffDelayMs(nextRetryCount) * 1_000_000)
                            )
            );
            return OutboxEventStatus.FAILED;
        }
    }

    private long computeBackoffDelayMs(int retryCount) {
        long delay = outboxProperties.getRetryBaseDelayMs();
        for (int i = 1; i < retryCount; i++) {
            if (delay >= outboxProperties.getRetryMaxDelayMs()) {
                return outboxProperties.getRetryMaxDelayMs();
            }
            delay = Math.min(delay * 2, outboxProperties.getRetryMaxDelayMs());
        }
        return delay;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }
}
