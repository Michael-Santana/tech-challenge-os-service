package com.tech.challenger.execution.service.os.application.service;

import com.tech.challenger.execution.service.os.application.event.OrderOutbox;
import com.tech.challenger.execution.service.os.application.event.SagaEvent;
import com.tech.challenger.execution.service.os.application.exception.ResourceNotFoundException;
import com.tech.challenger.execution.service.os.domain.model.Order;
import com.tech.challenger.execution.service.os.domain.model.OrderEvent;
import com.tech.challenger.execution.service.os.domain.model.OrderStatus;
import com.tech.challenger.execution.service.os.domain.repository.OrderRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SagaEventHandler {

    private final OrderRepository orders;
    private final JdbcTemplate jdbc;
    private final OrderOutbox outbox;

    public SagaEventHandler(OrderRepository orders, JdbcTemplate jdbc, OrderOutbox outbox) {
        this.orders = orders;
        this.jdbc = jdbc;
        this.outbox = outbox;
    }

    @Transactional
    public void handle(SagaEvent event) {
        if (event == null || event.eventId() == null || event.eventId().isBlank()
                || event.eventType() == null || event.orderId() == null) {
            throw new IllegalArgumentException("Evento da saga sem identificacao, tipo ou OS");
        }
        OrderStatus target = switch (event.eventType()) {
            case "EXECUTION_DIAGNOSIS_STARTED" -> OrderStatus.EM_DIAGNOSTICO;
            case "EXECUTION_DIAGNOSIS_COMPLETED" -> OrderStatus.AGUARDANDO_APROVACAO;
            case "EXECUTION_STARTED" -> OrderStatus.EM_EXECUCAO;
            case "EXECUTION_COMPLETED" -> OrderStatus.FINALIZADA;
            case "BUDGET_REJECTED" -> OrderStatus.CANCELADA;
            default -> throw new IllegalArgumentException("Tipo de evento desconhecido: " + event.eventType());
        };
        if (event.status() != null && !target.name().equals(event.status())) {
            throw new IllegalArgumentException("Status inconsistente com o tipo do evento");
        }

        int inserted = jdbc.update("INSERT INTO saga_inbox (event_id) VALUES (?) ON CONFLICT DO NOTHING",
                event.eventId());
        if (inserted == 0) {
            return;
        }
        Order order = orders.findByIdForUpdate(event.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de servico nao encontrada: " + event.orderId()));
        OrderStatus previous = order.getStatus();
        if (previous == target) {
            return;
        }
        if (!canTransition(previous, target)) {
            throw new IllegalStateException("Transicao de saga invalida: " + previous + " -> " + target);
        }
        order.setStatus(target);
        if (target == OrderStatus.FINALIZADA) {
            order.setFinishedAt(LocalDateTime.now());
        }
        orders.save(order);
        outbox.enqueue(OrderEvent.builder().id(UUID.randomUUID().toString()).orderId(order.getId())
                .eventType("STATUS_UPDATED").previousStatus(previous).newStatus(target)
                .description(event.description() == null ? event.eventType() : event.description())
                .occurredAt(LocalDateTime.now()).build());
    }

    private boolean canTransition(OrderStatus previous, OrderStatus target) {
        return switch (target) {
            case EM_DIAGNOSTICO -> previous == OrderStatus.RECEBIDA;
            case AGUARDANDO_APROVACAO -> previous == OrderStatus.EM_DIAGNOSTICO;
            case EM_EXECUCAO -> previous == OrderStatus.AGUARDANDO_APROVACAO;
            case FINALIZADA -> previous == OrderStatus.EM_EXECUCAO;
            case CANCELADA -> previous == OrderStatus.RECEBIDA
                    || previous == OrderStatus.EM_DIAGNOSTICO
                    || previous == OrderStatus.AGUARDANDO_APROVACAO;
            default -> false;
        };
    }
}
