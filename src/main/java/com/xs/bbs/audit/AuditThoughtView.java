package com.xs.bbs.audit;

import com.xs.bbs.ai.ThoughtAiProfileView;
import com.xs.bbs.thought.ThoughtDegree;

import java.time.LocalDateTime;
import java.util.List;

public record AuditThoughtView(
        Long thoughtId,
        Long userId,
        String userNickname,
        String content,
        ThoughtDegree degree,
        LocalDateTime createdAt,
        ThoughtAiProfileView analysis,
        List<AuditRecordView> records
) {
}
