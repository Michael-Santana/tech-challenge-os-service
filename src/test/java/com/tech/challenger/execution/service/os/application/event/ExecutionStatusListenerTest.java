package com.tech.challenger.execution.service.os.application.event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.tech.challenger.execution.service.os.application.dto.ExecutionStatusEvent;
import com.tech.challenger.execution.service.os.application.service.OrderServiceImpl;
import com.tech.challenger.execution.service.os.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;

class ExecutionStatusListenerTest {

    @Test
    void forwardsExecutionStatusForOrderServiceToPersist() {
        OrderServiceImpl orderService = mock(OrderServiceImpl.class);
        ExecutionStatusListener listener = new ExecutionStatusListener(orderService);

        listener.receive(new ExecutionStatusEvent(
                "event-1", "EXECUTION_COMPLETED", 1L, "FINALIZADA", "Execucao concluida", "now"));

        verify(orderService).applyExecutionStatus(1L, OrderStatus.FINALIZADA, "Execucao concluida");
    }

    @Test
    void rejectsStatusesOutsideExecutionWorkflow() {
        ExecutionStatusListener listener = new ExecutionStatusListener(mock(OrderServiceImpl.class));

        assertThrows(IllegalArgumentException.class, () -> listener.receive(
                new ExecutionStatusEvent("event-2", "INVALID", 2L, "CANCELADA", "Invalid", "now")));
    }
}
