package com.xs.bbs.audit;

import com.xs.bbs.ai.ThoughtModerationStatus;

import java.time.LocalDateTime;

public record AuditRecordView(
        Long id,
        Long thoughtId,
        Long operatorUserId,
        String operatorDisplayName,
        AuditSourceType sourceType,
        ThoughtModerationStatus previousStatus,
        ThoughtModerationStatus currentStatus,
        String decisionReason,
        LocalDateTime createdAt
) {
}
