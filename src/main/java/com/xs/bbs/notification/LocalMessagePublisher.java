package com.xs.bbs.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!rocketmq")
public class LocalMessagePublisher implements MessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(LocalMessagePublisher.class);
    private final NotificationDeliveryHandler notificationDeliveryHandler;

    public LocalMessagePublisher(NotificationDeliveryHandler notificationDeliveryHandler) {
        this.notificationDeliveryHandler = notificationDeliveryHandler;
    }

    @Override
    public void publish(NotificationDispatchPayload payload) {
        log.info("local notification dispatched: receiverId={}, title={}", payload.receiverId(), payload.title());
        notificationDeliveryHandler.deliver(payload);
    }
}
