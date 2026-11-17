package com.tech.challenger.execution.service.os.application.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tech.challenger.execution.service.os.domain.model.OrderEvent;
import com.tech.challenger.execution.service.os.domain.repository.OrderEventRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderOutboxDispatcher {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final OrderEventRepository history;
    private final RabbitTemplate rabbit;
    private final String exchange;
    private final String routingKey;

    public OrderOutboxDispatcher(JdbcTemplate jdbc, ObjectMapper objectMapper,
            OrderEventRepository history, RabbitTemplate rabbit,
            @Value("${app.rabbitmq.exchange}") String exchange,
            @Value("${app.rabbitmq.routing-key}") String routingKey) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.history = history;
        this.rabbit = rabbit;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Scheduled(fixedDelayString = "${app.outbox.delay-ms}")
    @Transactional
    public void dispatch() {
        List<String> payloads = jdbc.queryForList(
                "SELECT payload FROM order_outbox WHERE published_at IS NULL "
                        + "ORDER BY created_at, event_id LIMIT 1 FOR UPDATE",
                String.class);
        if (payloads.isEmpty()) {
            return;
        }

        OrderEvent event;
        try {
            event = objectMapper.readValue(payloads.getFirst(), OrderEvent.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Evento da outbox invalido", exception);
        }

        history.save(event);
        CorrelationData correlation = new CorrelationData(event.getId());
        rabbit.convertAndSend(exchange, routingKey, event, correlation);
        try {
            var confirmation = correlation.getFuture().get(10, TimeUnit.SECONDS);
            if (!confirmation.isAck() || correlation.getReturned() != null) {
                throw new IllegalStateException("RabbitMQ nao confirmou o evento " + event.getId());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Publicacao interrompida", exception);
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException exception) {
            throw new IllegalStateException("Falha aguardando confirmacao do RabbitMQ", exception);
        }
        jdbc.update("UPDATE order_outbox SET published_at = ? WHERE event_id = ?",
                LocalDateTime.now(), event.getId());
    }
}
