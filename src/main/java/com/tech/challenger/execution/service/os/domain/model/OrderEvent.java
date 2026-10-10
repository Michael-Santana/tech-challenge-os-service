package com.tech.challenger.execution.service.os.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Evento de historico/auditoria da Ordem de Servico.
 * Persistido no MongoDB para trilha de auditoria.
 */
@Document(collection = "order_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEvent {

    @Id
    private String id;

    private Long orderId;

    private String eventType;

    private OrderStatus previousStatus;

    private OrderStatus newStatus;

    private String description;

    private LocalDateTime occurredAt;
}

