package com.tech.challenger.execution.service.os.application.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tech.challenger.execution.service.os.domain.model.OrderEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderOutbox {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public OrderOutbox(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(OrderEvent event) {
        try {
            jdbc.update("INSERT INTO order_outbox (event_id, payload, created_at) VALUES (?, ?, ?)",
                    event.getId(), objectMapper.writeValueAsString(event), event.getOccurredAt());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Nao foi possivel serializar o evento da OS", exception);
        }
    }
}
