package com.xs.bbs.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OutboxEventDispatcher {

    private final ObjectMapper objectMapper;
    private final Map<OutboxEventType, OutboxEventHandler<?>> handlers;

    public OutboxEventDispatcher(ObjectMapper objectMapper, List<OutboxEventHandler<?>> handlers) {
        this.objectMapper = objectMapper;
        this.handlers = handlers.stream().collect(Collectors.toMap(OutboxEventHandler::eventType, Function.identity()));
    }

    public String dispatch(OutboxEventType eventType, String payloadJson) throws Exception {
        OutboxEventHandler<?> handler = handlers.get(eventType);
        if (handler == null) {
            throw new IllegalArgumentException("missing handler for event type: " + eventType);
        }
        return handleEvent(handler, payloadJson);
    }

    private <T> String handleEvent(OutboxEventHandler<T> handler, String payloadJson) throws Exception {
        T payload = objectMapper.readValue(payloadJson, handler.payloadType());
        return handler.handle(payload);
    }
}
