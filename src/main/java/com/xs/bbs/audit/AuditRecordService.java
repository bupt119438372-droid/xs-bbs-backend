package com.xs.bbs.audit;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xs.bbs.ai.ThoughtAiProfileEntity;
import com.xs.bbs.ai.ThoughtAiProfileMapper;
import com.xs.bbs.ai.ThoughtModerationStatus;
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
public class AuditRecordService {

    private final AuditRecordMapper auditRecordMapper;
    private final UserService userService;
    private final ThoughtRepository thoughtRepository;
    private final ThoughtAiProfileMapper thoughtAiProfileMapper;

    public AuditRecordService(
            AuditRecordMapper auditRecordMapper,
            UserService userService,
            ThoughtRepository thoughtRepository,
            ThoughtAiProfileMapper thoughtAiProfileMapper
    ) {
        this.auditRecordMapper = auditRecordMapper;
        this.userService = userService;
        this.thoughtRepository = thoughtRepository;
        this.thoughtAiProfileMapper = thoughtAiProfileMapper;
    }

    public void recordAiDecision(Long thoughtId, ThoughtModerationStatus currentStatus, String reason) {
        AuditRecordEntity entity = new AuditRecordEntity();
        entity.setThoughtId(thoughtId);
        entity.setOperatorUserId(null);
        entity.setSourceType(AuditSourceType.AI.name());
        entity.setPreviousStatus(null);
        entity.setCurrentStatus(currentStatus.name());
        entity.setDecisionReason(reason);
        entity.setCreatedAt(LocalDateTime.now());
        auditRecordMapper.insert(entity);
    }

    public void recordAdminDecision(
            Long thoughtId,
            Long operatorUserId,
            ThoughtModerationStatus previousStatus,
            ThoughtModerationStatus currentStatus,
            String reason
    ) {
        AuditRecordEntity entity = new AuditRecordEntity();
        entity.setThoughtId(thoughtId);
        entity.setOperatorUserId(operatorUserId);
        entity.setSourceType(AuditSourceType.ADMIN.name());
        entity.setPreviousStatus(previousStatus == null ? null : previousStatus.name());
        entity.setCurrentStatus(currentStatus.name());
        entity.setDecisionReason(reason);
        entity.setCreatedAt(LocalDateTime.now());
        auditRecordMapper.insert(entity);
    }

    public List<AuditRecordView> listByThoughtId(Long thoughtId) {
        return listRecords(null, null, null, thoughtId, null, null, null);
    }

    public List<AuditRecordView> listRecords(
            AuditSourceType sourceType,
            String operatorKeyword,
            ThoughtModerationStatus currentStatus,
            Long thoughtId,
            String keyword,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        return auditRecordMapper.selectList(
                        Wrappers.lambdaQuery(AuditRecordEntity.class)
                                .eq(thoughtId != null, AuditRecordEntity::getThoughtId, thoughtId)
                                .eq(sourceType != null, AuditRecordEntity::getSourceType, sourceType == null ? null : sourceType.name())
                                .eq(currentStatus != null, AuditRecordEntity::getCurrentStatus, currentStatus == null ? null : currentStatus.name())
                                .ge(dateFrom != null, AuditRecordEntity::getCreatedAt, dateFrom == null ? null : dateFrom.atStartOfDay())
                                .lt(dateTo != null, AuditRecordEntity::getCreatedAt, dateTo == null ? null : dateTo.plusDays(1).atStartOfDay())
                                .orderByDesc(AuditRecordEntity::getCreatedAt)
                ).stream()
                .map(this::toView)
                .filter(view -> matchesOperatorKeyword(view, operatorKeyword))
                .filter(view -> matchesKeyword(view, keyword))
                .toList();
    }

    public long countBySourceType(AuditSourceType sourceType) {
        return auditRecordMapper.selectCount(
                Wrappers.lambdaQuery(AuditRecordEntity.class)
                        .eq(AuditRecordEntity::getSourceType, sourceType.name())
        );
    }

    public long countOverriddenDecisions() {
        return auditRecordMapper.selectCount(
                Wrappers.lambdaQuery(AuditRecordEntity.class)
                        .eq(AuditRecordEntity::getSourceType, AuditSourceType.ADMIN.name())
                        .isNotNull(AuditRecordEntity::getPreviousStatus)
                        .apply("previous_status <> current_status")
        );
    }

    public long countTodayDecisions() {
        return auditRecordMapper.selectCount(
                Wrappers.lambdaQuery(AuditRecordEntity.class)
                        .ge(AuditRecordEntity::getCreatedAt, LocalDate.now().atStartOfDay())
        );
    }

    private boolean matchesOperatorKeyword(AuditRecordView view, String operatorKeyword) {
        if (operatorKeyword == null || operatorKeyword.isBlank()) {
            return true;
        }
        return view.operatorDisplayName().contains(operatorKeyword.trim());
    }

    private boolean matchesKeyword(AuditRecordView view, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        ThoughtPost thought = thoughtRepository.findById(view.thoughtId()).orElse(null);
        if (thought == null) {
            return SearchSupport.matches(keyword, view.operatorDisplayName(), view.decisionReason(), String.valueOf(view.thoughtId()));
        }
        UserProfile owner = userService.getRequiredUser(thought.userId());
        ThoughtAiProfileEntity analysis = thoughtAiProfileMapper.selectById(thought.id());
        return SearchSupport.matches(
                keyword,
                view.operatorDisplayName(),
                view.decisionReason(),
                String.valueOf(view.thoughtId()),
                thought.content(),
                owner.nickname(),
                analysis == null ? null : analysis.getSummary(),
                analysis == null ? null : analysis.getTagsJson()
        );
    }

    private AuditRecordView toView(AuditRecordEntity entity) {
        UserProfile operator = entity.getOperatorUserId() == null
                ? null
                : userService.getUser(entity.getOperatorUserId());
        return new AuditRecordView(
                entity.getId(),
                entity.getThoughtId(),
                entity.getOperatorUserId(),
                operator == null ? "AI 审核" : operator.nickname(),
                AuditSourceType.valueOf(entity.getSourceType()),
                entity.getPreviousStatus() == null ? null : ThoughtModerationStatus.valueOf(entity.getPreviousStatus()),
                ThoughtModerationStatus.valueOf(entity.getCurrentStatus()),
                entity.getDecisionReason(),
                entity.getCreatedAt()
        );
    }
}
