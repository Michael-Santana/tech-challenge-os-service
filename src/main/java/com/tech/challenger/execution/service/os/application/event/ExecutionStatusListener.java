package com.tech.challenger.execution.service.os.application.event;

import com.tech.challenger.execution.service.os.application.dto.ExecutionStatusEvent;
import com.tech.challenger.execution.service.os.application.service.OrderServiceImpl;
import com.tech.challenger.execution.service.os.domain.model.OrderStatus;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ExecutionStatusListener {

    private final OrderServiceImpl orderService;

    public ExecutionStatusListener(OrderServiceImpl orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = "${app.rabbitmq.execution-status-queue:os.execution-status.queue}")
    public void receive(ExecutionStatusEvent event) {
        if (event == null || event.orderId() == null || event.status() == null) {
            throw new IllegalArgumentException("Evento de execucao sem orderId ou status");
        }
        OrderStatus status;
        try {
            status = OrderStatus.valueOf(event.status());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Status de execucao desconhecido: " + event.status(), exception);
        }
        if (status != OrderStatus.EM_DIAGNOSTICO
                && status != OrderStatus.AGUARDANDO_APROVACAO
                && status != OrderStatus.EM_EXECUCAO
                && status != OrderStatus.FINALIZADA) {
            throw new IllegalArgumentException("Status nao pode ser alterado pelo Execution Service: " + status);
        }
        orderService.applyExecutionStatus(event.orderId(), status, event.description());
    }
}
