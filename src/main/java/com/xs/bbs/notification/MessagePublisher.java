package com.xs.bbs.notification;

public interface MessagePublisher {

    void publish(NotificationDispatchPayload payload);
}
