package com.tech.challenger.execution.service.os.infrastructure.config;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SagaMessagingConfig {

    @Bean
    Declarables sagaQueues(@Value("${app.rabbitmq.exchange}") String osExchange,
                           @Value("${app.rabbitmq.billing-exchange:billing.exchange}") String billingExchange) {
        var os = new TopicExchange(osExchange, true, false);
        var billing = new TopicExchange(billingExchange, true, false);
        var dlx = new TopicExchange("os.saga.dlx", true, false);
        var queue = QueueBuilder.durable("os.saga.events")
                .deadLetterExchange("os.saga.dlx").deadLetterRoutingKey("failed").build();
        var dead = QueueBuilder.durable("os.saga.events.dlq").build();
        return new Declarables(os, billing, dlx, queue, dead,
                BindingBuilder.bind(queue).to(os).with("execution.status"),
                BindingBuilder.bind(queue).to(billing).with("billing.budget_rejected"),
                BindingBuilder.bind(dead).to(dlx).with("failed"));
    }
}
