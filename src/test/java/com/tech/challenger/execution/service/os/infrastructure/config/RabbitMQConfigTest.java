package com.tech.challenger.execution.service.os.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.tech.challenger.execution.service.os.domain.model.OrderEvent;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;

class RabbitMQConfigTest {

    @Test
    void serializesEventTimestampAsIsoStringForOtherServices() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var converter = new RabbitMQConfig().jsonMessageConverter(mapper);
        var event = OrderEvent.builder().id("event-1").orderId(1L).eventType("ORDER_CREATED")
                .occurredAt(LocalDateTime.of(2026, 11, 17, 7, 0)).build();
        var message = converter.toMessage(event, new MessageProperties());

        assertEquals("2026-11-17T07:00:00", mapper.readTree(message.getBody()).get("occurredAt").asText());
    }
}
