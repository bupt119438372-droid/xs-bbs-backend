package com.xs.bbs.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingNotificationDeliveryHandler implements NotificationDeliveryHandler {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationDeliveryHandler.class);

    @Override
    public void deliver(NotificationDispatchPayload payload) {
        log.info(
                "notification delivered: notificationId={}, receiverId={}, senderId={}, type={}, title={}",
                payload.notificationId(),
                payload.receiverId(),
                payload.senderId(),
                payload.type(),
                payload.title()
        );
    }
}
