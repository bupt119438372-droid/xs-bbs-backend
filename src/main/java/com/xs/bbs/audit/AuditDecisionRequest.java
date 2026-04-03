package com.xs.bbs.audit;

import com.xs.bbs.ai.ThoughtModerationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AuditDecisionRequest(
        @NotNull ThoughtModerationStatus moderationStatus,
        @Size(max = 255) String moderationReason
) {
}
