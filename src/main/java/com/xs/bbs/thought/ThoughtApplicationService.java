package com.xs.bbs.thought;

import com.xs.bbs.insight.InsightService;
import com.xs.bbs.outbox.OutboxEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ThoughtApplicationService {

    private final ThoughtRepository thoughtRepository;
    private final InsightService insightService;
    private final OutboxEventPublisher outboxEventPublisher;

    public ThoughtApplicationService(
            ThoughtRepository thoughtRepository,
            InsightService insightService,
            OutboxEventPublisher outboxEventPublisher
    ) {
        this.thoughtRepository = thoughtRepository;
        this.insightService = insightService;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    @Transactional
    public ThoughtPost publish(Long userId, PublishThoughtRequest request) {
        ThoughtPost thought = thoughtRepository.save(
                userId,
                request.content(),
                request.degree(),
                request.allowRecommendation(),
                request.publicVisible()
        );
        insightService.evictDailyInsight(userId);
        outboxEventPublisher.publishThoughtPublished(thought);
        return thought;
    }
}
