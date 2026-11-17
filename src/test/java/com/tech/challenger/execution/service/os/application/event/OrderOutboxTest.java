package com.tech.challenger.execution.service.os.application.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tech.challenger.execution.service.os.domain.model.OrderEvent;
import com.tech.challenger.execution.service.os.domain.model.OrderStatus;
import com.tech.challenger.execution.service.os.domain.repository.OrderEventRepository;
import java.time.LocalDateTime;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class OrderOutboxTest {

    private JdbcTemplate jdbc;
    private TransactionTemplate transaction;
    private OrderOutbox outbox;
    private OrderOutboxDispatcher dispatcher;
    private OrderEventRepository history;
    private RabbitTemplate rabbit;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + java.util.UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE order_outbox (event_id VARCHAR(36) PRIMARY KEY, payload CLOB NOT NULL, "
                + "created_at TIMESTAMP NOT NULL, published_at TIMESTAMP)");
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        outbox = new OrderOutbox(jdbc, mapper);
        history = mock(OrderEventRepository.class);
        rabbit = mock(RabbitTemplate.class);
        dispatcher = new OrderOutboxDispatcher(jdbc, mapper, history, rabbit, "os.exchange", "os.event");
    }

    private OrderEvent event() {
        return OrderEvent.builder().id("event-1").orderId(1L).eventType("ORDER_CREATED")
                .newStatus(OrderStatus.RECEBIDA).occurredAt(LocalDateTime.now()).build();
    }

    @Test
    void rollsBackEventWithOrderTransaction() {
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            outbox.enqueue(event());
            throw new IllegalStateException("rollback");
        }));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM order_outbox", Integer.class));
    }

    @Test
    void keepsEventPendingOnFailureAndRetriesWithSameId() {
        transaction.executeWithoutResult(status -> outbox.enqueue(event()));
        doThrow(new IllegalStateException("broker unavailable"))
                .doAnswer(invocation -> {
                    CorrelationData correlation = invocation.getArgument(3);
                    correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
                    return null;
                })
                .when(rabbit).convertAndSend(eq("os.exchange"), eq("os.event"),
                        any(OrderEvent.class), any(CorrelationData.class));

        assertThrows(IllegalStateException.class,
                () -> transaction.executeWithoutResult(status -> dispatcher.dispatch()));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM order_outbox WHERE published_at IS NULL", Integer.class));

        transaction.executeWithoutResult(status -> dispatcher.dispatch());
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM order_outbox WHERE published_at IS NULL", Integer.class));
        verify(history, org.mockito.Mockito.times(2)).save(any(OrderEvent.class));
    }
}
