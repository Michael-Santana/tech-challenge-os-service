package com.tech.challenger.execution.service.os.application.service;

import com.tech.challenger.execution.service.os.application.dto.CreateOrderRequest;
import com.tech.challenger.execution.service.os.application.dto.OrderEventResponse;
import com.tech.challenger.execution.service.os.application.dto.OrderResponse;
import com.tech.challenger.execution.service.os.application.dto.UpdateOrderStatusRequest;
import com.tech.challenger.execution.service.os.application.event.OrderEventPublisher;
import com.tech.challenger.execution.service.os.application.exception.ResourceNotFoundException;
import com.tech.challenger.execution.service.os.domain.model.Order;
import com.tech.challenger.execution.service.os.domain.model.OrderEvent;
import com.tech.challenger.execution.service.os.domain.model.OrderStatus;
import com.tech.challenger.execution.service.os.domain.repository.OrderEventRepository;
import com.tech.challenger.execution.service.os.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl {

    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final OrderEventPublisher eventPublisher;
    private final SagaOrchestrator sagaOrchestrator;

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .vehicleId(request.getVehicleId())
                .description(request.getDescription())
                .status(OrderStatus.RECEBIDA)
                .build();
        order = orderRepository.save(order);

        registerEvent(order, "ORDER_CREATED", null, order.getStatus(),
                "Ordem de servico criada");

        // Inicia a saga de criacao da OS (valida cliente/veiculo, reserva de pecas, etc.)
        sagaOrchestrator.startOrderCreationSaga(order);

        return toResponse(order);
    }

    public OrderResponse findById(Long id) {
        return toResponse(getOrder(id));
    }

    @Transactional
    public OrderResponse updateStatus(Long id, UpdateOrderStatusRequest request) {
        Order order = getOrder(id);
        OrderStatus previousStatus = order.getStatus();

        validateStatusTransition(previousStatus, request.getStatus());

        order.setStatus(request.getStatus());
        if (request.getStatus() == OrderStatus.FINALIZADA
                || request.getStatus() == OrderStatus.ENTREGUE) {
            order.setFinishedAt(LocalDateTime.now());
        }
        order = orderRepository.save(order);

        registerEvent(order, "STATUS_UPDATED", previousStatus, order.getStatus(),
                request.getDescription() != null ? request.getDescription()
                        : "Status alterado de " + previousStatus + " para " + order.getStatus());

        return toResponse(order);
    }

    public List<OrderEventResponse> getHistory(Long id) {
        getOrder(id); // garante que a OS existe
        return orderEventRepository.findByOrderIdOrderByOccurredAtAsc(id)
                .stream()
                .map(this::toEventResponse)
                .toList();
    }

    public Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de servico nao encontrada: " + id));
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        if (current == OrderStatus.ENTREGUE || current == OrderStatus.CANCELADA) {
            throw new IllegalStateException(
                    "Nao e possivel alterar o status de uma OS " + current);
        }
        if (current == next) {
            throw new IllegalArgumentException("A OS ja esta com o status " + next);
        }
    }

    private void registerEvent(Order order, String eventType, OrderStatus previousStatus,
                               OrderStatus newStatus, String description) {
        OrderEvent event = OrderEvent.builder()
                .orderId(order.getId())
                .eventType(eventType)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .description(description)
                .occurredAt(LocalDateTime.now())
                .build();
        orderEventRepository.save(event);
        eventPublisher.publish(event);
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .vehicleId(order.getVehicleId())
                .status(order.getStatus())
                .description(order.getDescription())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .finishedAt(order.getFinishedAt())
                .build();
    }

    private OrderEventResponse toEventResponse(OrderEvent event) {
        return OrderEventResponse.builder()
                .id(event.getId())
                .orderId(event.getOrderId())
                .eventType(event.getEventType())
                .previousStatus(event.getPreviousStatus())
                .newStatus(event.getNewStatus())
                .description(event.getDescription())
                .occurredAt(event.getOccurredAt())
                .build();
    }
}

