package com.tech.challenger.execution.service.os.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tech.challenger.execution.service.os.application.event.SagaEvent;
import com.tech.challenger.execution.service.os.application.service.SagaEventHandler;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

class SagaMessagingConfigTest {

    @Test
    void declaresSagaQueuesBindingsAndDeadLetterExchange() {
        var declarables = new SagaMessagingConfig().sagaQueues("test.os", "test.billing");
        var exchanges = declarables.getDeclarablesByType(TopicExchange.class);
        var queues = declarables.getDeclarablesByType(Queue.class);
        var bindings = declarables.getDeclarablesByType(Binding.class);

        assertEquals(3, exchanges.size());
        assertTrue(exchanges.stream().anyMatch(exchange -> exchange.getName().equals("test.os")));
        assertTrue(exchanges.stream().anyMatch(exchange -> exchange.getName().equals("test.billing")));
        assertEquals(2, queues.size());
        assertTrue(queues.stream().anyMatch(queue -> queue.getName().equals("os.saga.events.dlq")));
        assertEquals(3, bindings.size());
        assertTrue(bindings.stream().anyMatch(binding -> binding.getRoutingKey().equals("execution.status")));
        assertTrue(bindings.stream().anyMatch(binding -> binding.getRoutingKey().equals("billing.budget_rejected")));
    }

    @Test
    void listenerDelegatesToSagaHandler() {
        var handler = mock(SagaEventHandler.class);
        var listener = new SagaEventListener(handler);
        var event = new SagaEvent("event", "EXECUTION_STARTED", 1L, "EM_EXECUCAO", null);

        listener.receive(event);

        verify(handler).handle(event);
    }
}
