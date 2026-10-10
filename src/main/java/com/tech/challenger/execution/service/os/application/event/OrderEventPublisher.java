package com.tech.challenger.execution.service.os.application.event;

import com.tech.challenger.execution.service.os.domain.model.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Publicador de eventos da Ordem de Servico no RabbitMQ.
 * Responsavel por notificar outros microsservicos sobre mudancas na OS.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange:os.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key:os.event}")
    private String routingKey;

    public void publish(OrderEvent event) {
        try {
            log.info("Publicando evento da OS {} - tipo: {}", event.getOrderId(), event.getEventType());
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
        } catch (Exception ex) {
            // Falha na mensageria nao deve impedir a operacao principal
            log.error("Falha ao publicar evento da OS {}: {}", event.getOrderId(), ex.getMessage());
        }
    }
}

