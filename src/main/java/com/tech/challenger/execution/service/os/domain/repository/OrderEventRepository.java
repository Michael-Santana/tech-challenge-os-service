package com.tech.challenger.execution.service.os.domain.repository;

import com.tech.challenger.execution.service.os.domain.model.OrderEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrderEventRepository extends MongoRepository<OrderEvent, String> {

    List<OrderEvent> findByOrderIdOrderByOccurredAtAsc(Long orderId);
}

