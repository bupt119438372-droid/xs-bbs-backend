package com.xs.bbs.audit;

public record AuditSummaryView(
        long total,
        long approved,
        long review,
        long rejected,
        long aiDecisions,
        long adminDecisions,
        long overriddenDecisions,
        long todayDecisions
) {
}
