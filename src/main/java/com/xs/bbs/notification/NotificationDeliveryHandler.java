package com.xs.bbs.notification;

public interface NotificationDeliveryHandler {

    void deliver(NotificationDispatchPayload payload);
}
