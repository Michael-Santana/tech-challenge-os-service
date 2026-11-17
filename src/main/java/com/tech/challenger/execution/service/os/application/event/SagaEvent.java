package com.tech.challenger.execution.service.os.application.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SagaEvent(String eventId, String eventType, Long orderId, String status, String description) {
}
