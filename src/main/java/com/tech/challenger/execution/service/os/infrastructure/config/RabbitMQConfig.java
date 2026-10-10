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

    @Bean
    public TopicExchange osExchange() {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue osEventsQueue() {
        return new Queue(queue, true);
    }

    @Bean
    public Binding osEventsBinding(Queue osEventsQueue, TopicExchange osExchange) {
        return BindingBuilder.bind(osEventsQueue).to(osExchange).with(routingKey);
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

