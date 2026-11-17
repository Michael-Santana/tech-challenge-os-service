package com.tech.challenger.execution.service.os.infrastructure.config;

import com.tech.challenger.execution.service.os.application.event.SagaEvent;
import com.tech.challenger.execution.service.os.application.service.SagaEventHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class SagaEventListener {

    private final SagaEventHandler handler;

    public SagaEventListener(SagaEventHandler handler) {
        this.handler = handler;
    }

    @RabbitListener(queues = "os.saga.events")
    public void receive(SagaEvent event) {
        handler.handle(event);
    }
}
