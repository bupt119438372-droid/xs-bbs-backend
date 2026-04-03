package com.xs.bbs;

import com.xs.bbs.outbox.OutboxEventMapper;
import com.xs.bbs.outbox.OutboxEventStatus;
import com.xs.bbs.thought.PublishThoughtRequest;
import com.xs.bbs.thought.ThoughtApplicationService;
import com.xs.bbs.thought.ThoughtDegree;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OutboxIntegrationTest {

    @Autowired
    private ThoughtApplicationService thoughtApplicationService;

    @Autowired
    private OutboxEventMapper outboxEventMapper;

    @Test
    void shouldPersistAndProcessThoughtPublishedEvent() {
        thoughtApplicationService.publish(
                4L,
                new PublishThoughtRequest("真正重要的不是快，而是别把方向交给焦虑。", ThoughtDegree.OBSESSION)
        );

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(outboxEventMapper.selectList(null))
                        .anySatisfy(event -> {
                            assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PROCESSED.name());
                            assertThat(event.getResultSummary()).contains("notifications=");
                        }));
    }
}
