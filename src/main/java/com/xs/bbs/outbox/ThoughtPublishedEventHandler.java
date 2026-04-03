package com.xs.bbs.outbox;

import com.xs.bbs.ai.ThoughtAiProfile;
import com.xs.bbs.ai.ThoughtAiService;
import com.xs.bbs.match.MatchCandidateView;
import com.xs.bbs.match.MatchService;
import com.xs.bbs.notification.NotificationCreateCommand;
import com.xs.bbs.notification.NotificationService;
import com.xs.bbs.notification.NotificationType;
import com.xs.bbs.thought.ThoughtPost;
import com.xs.bbs.thought.ThoughtRepository;
import com.xs.bbs.user.UserProfile;
import com.xs.bbs.user.UserService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ThoughtPublishedEventHandler implements OutboxEventHandler<ThoughtPublishedPayload> {

    private final ThoughtRepository thoughtRepository;
    private final MatchService matchService;
    private final NotificationService notificationService;
    private final UserService userService;
    private final ThoughtAiService thoughtAiService;

    public ThoughtPublishedEventHandler(
            ThoughtRepository thoughtRepository,
            MatchService matchService,
            NotificationService notificationService,
            UserService userService,
            ThoughtAiService thoughtAiService
    ) {
        this.thoughtRepository = thoughtRepository;
        this.matchService = matchService;
        this.notificationService = notificationService;
        this.userService = userService;
        this.thoughtAiService = thoughtAiService;
    }

    @Override
    public OutboxEventType eventType() {
        return OutboxEventType.THOUGHT_PUBLISHED;
    }

    @Override
    public Class<ThoughtPublishedPayload> payloadType() {
        return ThoughtPublishedPayload.class;
    }

    @Override
    public String handle(ThoughtPublishedPayload payload) {
        ThoughtPost thought = thoughtRepository.findById(payload.thoughtId())
                .orElseThrow(() -> new IllegalArgumentException("thought does not exist"));
        ThoughtAiProfile analysis = thoughtAiService.ensureProfile(thought);
        if (!analysis.matchEligible()) {
            return "analysis-only moderation=" + analysis.moderationStatus();
        }
        List<MatchCandidateView> candidates = matchService.findCandidatesForThought(thought);
        UserProfile sender = userService.getRequiredUser(payload.userId());
        for (MatchCandidateView candidate : candidates) {
            String title = candidate.visibilityLevel().name().equals("REAL_NAME")
                    ? sender.nickname() + " 和你产生了同频"
                    : "有人和你产生了同频";
            String anchor = analysis.summary().isBlank() ? trim(thought.content(), 28) : analysis.summary();
            String content = "你的某条念头与“" + trim(anchor, 28) + "”高度相关。";
            notificationService.create(new NotificationCreateCommand(
                    candidate.targetUserId(),
                    payload.userId(),
                    payload.thoughtId(),
                    NotificationType.RELATIONSHIP,
                    title,
                    content
            ));
        }
        return "matched-candidates=" + candidates.size() + ", notifications=" + candidates.size();
    }

    private String trim(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
