package com.tech.challenger.execution.service.os.application.dto;

import com.tech.challenger.execution.service.os.domain.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEventResponse {

    private String id;
    private Long orderId;
    private String eventType;
    private OrderStatus previousStatus;
    private OrderStatus newStatus;
    private String description;
    private LocalDateTime occurredAt;
}

