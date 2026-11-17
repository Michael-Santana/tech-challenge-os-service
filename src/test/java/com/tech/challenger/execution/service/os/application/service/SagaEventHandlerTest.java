package com.tech.challenger.execution.service.os.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tech.challenger.execution.service.os.application.event.OrderOutbox;
import com.tech.challenger.execution.service.os.application.event.SagaEvent;
import com.tech.challenger.execution.service.os.domain.model.Order;
import com.tech.challenger.execution.service.os.domain.model.OrderStatus;
import com.tech.challenger.execution.service.os.domain.repository.OrderRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class SagaEventHandlerTest {

    @Mock OrderRepository orders;
    @Mock JdbcTemplate jdbc;
    @Mock OrderOutbox outbox;
    SagaEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SagaEventHandler(orders, jdbc, outbox);
    }

    @Test
    void appliesDiagnosisEventAndPublishesStatus() {
        var order = Order.builder().id(1L).status(OrderStatus.RECEBIDA).build();
        when(jdbc.update(anyString(), any(Object.class))).thenReturn(1);
        when(orders.findByIdForUpdate(1L)).thenReturn(Optional.of(order));

        handler.handle(new SagaEvent("event-1", "EXECUTION_DIAGNOSIS_STARTED", 1L, "EM_DIAGNOSTICO", null));

        assertEquals(OrderStatus.EM_DIAGNOSTICO, order.getStatus());
        verify(outbox).enqueue(any());
    }

    @Test
    void ignoresAlreadyProcessedMessage() {
        when(jdbc.update(anyString(), any(Object.class))).thenReturn(0);

        handler.handle(new SagaEvent("event-1", "EXECUTION_DIAGNOSIS_STARTED", 1L, "EM_DIAGNOSTICO", null));

        verify(orders, never()).findByIdForUpdate(any());
        verify(outbox, never()).enqueue(any());
    }

    @Test
    void rejectsTransitionsThatWouldUndoCompletedWork() {
        when(jdbc.update(anyString(), any(Object.class))).thenReturn(1);
        when(orders.findByIdForUpdate(1L)).thenReturn(
                Optional.of(Order.builder().id(1L).status(OrderStatus.FINALIZADA).build()));

        assertThrows(IllegalStateException.class,
                () -> handler.handle(new SagaEvent("event-2", "BUDGET_REJECTED", 1L, null, null)));
        verify(outbox, never()).enqueue(any());
    }
}
