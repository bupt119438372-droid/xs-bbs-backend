package com.xs.bbs.audit;

import com.xs.bbs.ai.ThoughtAiProfileView;
import com.xs.bbs.ai.ThoughtAiService;
import com.xs.bbs.ai.ThoughtModerationStatus;
import com.xs.bbs.common.PageResponse;
import com.xs.bbs.common.SearchSupport;
import com.xs.bbs.thought.ThoughtPost;
import com.xs.bbs.thought.ThoughtRepository;
import com.xs.bbs.user.UserProfile;
import com.xs.bbs.user.UserService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditService {

    private final ThoughtRepository thoughtRepository;
    private final ThoughtAiService thoughtAiService;
    private final UserService userService;
    private final AuditRecordService auditRecordService;

    public AuditService(
            ThoughtRepository thoughtRepository,
            ThoughtAiService thoughtAiService,
            UserService userService,
            AuditRecordService auditRecordService
    ) {
        this.thoughtRepository = thoughtRepository;
        this.thoughtAiService = thoughtAiService;
        this.userService = userService;
        this.auditRecordService = auditRecordService;
    }

    public List<AuditThoughtView> listThoughts(ThoughtModerationStatus status) {
        return listThoughts(status, null);
    }

    public List<AuditThoughtView> listThoughts(ThoughtModerationStatus status, String keyword) {
        return thoughtRepository.findAll().stream()
                .map(this::toView)
                .filter(view -> status == null || view.analysis().moderationStatus() == status)
                .filter(view -> SearchSupport.matches(
                        keyword,
                        view.userNickname(),
                        view.content(),
                        view.analysis().summary(),
                        String.join(" ", view.analysis().tags())
                ))
                .toList();
    }

    public AuditSummaryView summary() {
        List<AuditThoughtView> thoughts = listThoughts(null, null);
        long approved = thoughts.stream().filter(item -> item.analysis().moderationStatus() == ThoughtModerationStatus.APPROVED).count();
        long review = thoughts.stream().filter(item -> item.analysis().moderationStatus() == ThoughtModerationStatus.REVIEW).count();
        long rejected = thoughts.stream().filter(item -> item.analysis().moderationStatus() == ThoughtModerationStatus.REJECTED).count();
        return new AuditSummaryView(
                thoughts.size(),
                approved,
                review,
                rejected,
                auditRecordService.countBySourceType(AuditSourceType.AI),
                auditRecordService.countBySourceType(AuditSourceType.ADMIN),
                auditRecordService.countOverriddenDecisions(),
                auditRecordService.countTodayDecisions()
        );
    }

    public ThoughtAiProfileView decide(Long thoughtId, Long operatorUserId, AuditDecisionRequest request) {
        return thoughtAiService.overrideModeration(
                thoughtId,
                operatorUserId,
                request.moderationStatus(),
                request.moderationReason()
        );
    }

    public List<AuditRecordView> records(Long thoughtId) {
        return auditRecordService.listByThoughtId(thoughtId);
    }

    public List<AuditRecordView> records(
            AuditSourceType sourceType,
            String operatorKeyword,
            ThoughtModerationStatus currentStatus,
            Long thoughtId,
            String keyword,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        return auditRecordService.listRecords(sourceType, operatorKeyword, currentStatus, thoughtId, keyword, dateFrom, dateTo);
    }

    public PageResponse<AuditRecordView> recordPage(
            AuditSourceType sourceType,
            String operatorKeyword,
            ThoughtModerationStatus currentStatus,
            Long thoughtId,
            String keyword,
            LocalDate dateFrom,
            LocalDate dateTo,
            Integer pageNo,
            Integer pageSize
    ) {
        List<AuditRecordView> records = records(sourceType, operatorKeyword, currentStatus, thoughtId, keyword, dateFrom, dateTo);
        int resolvedPageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int resolvedPageSize = pageSize == null ? 10 : Math.max(1, Math.min(pageSize, 100));
        long total = records.size();
        long totalPages = total == 0 ? 0 : (total + resolvedPageSize - 1) / resolvedPageSize;
        int fromIndex = Math.min((resolvedPageNo - 1) * resolvedPageSize, records.size());
        int toIndex = Math.min(fromIndex + resolvedPageSize, records.size());
        List<AuditRecordView> items = records.subList(fromIndex, toIndex);
        return new PageResponse<>(
                items,
                total,
                resolvedPageNo,
                resolvedPageSize,
                totalPages,
                resolvedPageNo > 1 && totalPages > 0,
                resolvedPageNo < totalPages
        );
    }

    public String exportRecordsCsv(
            AuditSourceType sourceType,
            String operatorKeyword,
            ThoughtModerationStatus currentStatus,
            Long thoughtId,
            String keyword,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        List<AuditRecordView> records = records(sourceType, operatorKeyword, currentStatus, thoughtId, keyword, dateFrom, dateTo);
        StringBuilder builder = new StringBuilder("\uFEFF");
        builder.append("记录ID,念头ID,审核来源,操作人,原状态,当前状态,决策原因,创建时间\n");
        for (AuditRecordView record : records) {
            builder.append(csv(record.id()))
                    .append(',').append(csv(record.thoughtId()))
                    .append(',').append(csv(record.sourceType()))
                    .append(',').append(csv(record.operatorDisplayName()))
                    .append(',').append(csv(record.previousStatus()))
                    .append(',').append(csv(record.currentStatus()))
                    .append(',').append(csv(record.decisionReason()))
                    .append(',').append(csv(record.createdAt()))
                    .append('\n');
        }
        return builder.toString();
    }

    public String buildExportFilename() {
        LocalDateTime now = LocalDateTime.now();
        return "audit-records-%d%02d%02d-%02d%02d%02d.csv".formatted(
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                now.getHour(),
                now.getMinute(),
                now.getSecond()
        );
    }

    private AuditThoughtView toView(ThoughtPost thought) {
        ThoughtAiProfileView analysis = thoughtAiService.getView(thought.id());
        UserProfile user = userService.getRequiredUser(thought.userId());
        return new AuditThoughtView(
                thought.id(),
                thought.userId(),
                user.nickname(),
                thought.content(),
                thought.degree(),
                thought.createdAt(),
                analysis,
                auditRecordService.listByThoughtId(thought.id())
        );
    }

    private String csv(Object value) {
        String text = value == null ? "" : value.toString();
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
