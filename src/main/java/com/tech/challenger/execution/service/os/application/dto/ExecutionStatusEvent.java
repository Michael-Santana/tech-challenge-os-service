package com.tech.challenger.execution.service.os.application.dto;

public record ExecutionStatusEvent(
        String eventId,
        String eventType,
        Long orderId,
        String status,
        String description,
        String occurredAt) {
}
