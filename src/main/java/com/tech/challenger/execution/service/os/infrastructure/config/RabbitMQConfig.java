package com.tech.challenger.execution.service.os.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Configuracao do RabbitMQ para publicacao de eventos da OS.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange:os.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.queue:os.events.queue}")
    private String queue;

    @Value("${app.rabbitmq.routing-key:os.event}")
    private String routingKey;

    @Value("${app.rabbitmq.execution-status-queue:os.execution-status.queue}")
    private String executionStatusQueue;

    @Value("${app.rabbitmq.execution-routing-key:execution.status}")
    private String executionRoutingKey;

    @Bean
    public TopicExchange osExchange() {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue osEventsQueue() {
        return new Queue(queue, true);
    }

    @Bean
    public Queue executionStatusQueue() {
        return new Queue(executionStatusQueue, true);
    }

    @Bean
    public Binding osEventsBinding(@Qualifier("osEventsQueue") Queue osEventsQueue,
                                   @Qualifier("osExchange") TopicExchange osExchange) {
        return BindingBuilder.bind(osEventsQueue).to(osExchange).with(routingKey);
    }

    @Bean
    public Binding executionStatusBinding(
            @Qualifier("executionStatusQueue") Queue executionStatusQueue,
            @Qualifier("osExchange") TopicExchange osExchange) {
        return BindingBuilder.bind(executionStatusQueue).to(osExchange).with(executionRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }
}
