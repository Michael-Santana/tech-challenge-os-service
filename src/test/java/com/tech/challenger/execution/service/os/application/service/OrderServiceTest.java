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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventRepository orderEventRepository;

    @Mock
    private OrderEventPublisher eventPublisher;

    @Mock
    private SagaOrchestrator sagaOrchestrator;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    @DisplayName("create deve salvar OS com status RECEBIDA, registrar evento e iniciar saga")
    void create_deveSalvarComStatusRecebida() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        request.setVehicleId(2L);
        request.setDescription("Revisao geral");

        Order saved = Order.builder()
                .id(1L)
                .customerId(1L)
                .vehicleId(2L)
                .description("Revisao geral")
                .status(OrderStatus.RECEBIDA)
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(saved);

        OrderResponse response = orderService.create(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(OrderStatus.RECEBIDA, response.getStatus());
        verify(orderEventRepository).save(any(OrderEvent.class));
        verify(eventPublisher).publish(any(OrderEvent.class));
        verify(sagaOrchestrator).startOrderCreationSaga(any(Order.class));
    }

    @Test
    @DisplayName("findById deve retornar OS existente")
    void findById_deveRetornar() {
        Order order = Order.builder().id(1L).status(OrderStatus.RECEBIDA).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.findById(1L);

        assertEquals(1L, response.getId());
    }

    @Test
    @DisplayName("findById deve lancar excecao quando nao encontrada")
    void findById_naoEncontrada_deveLancarExcecao() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.findById(99L));
    }

    @Test
    @DisplayName("updateStatus deve alterar status e registrar evento")
    void updateStatus_deveAlterarStatus() {
        Order order = Order.builder().id(1L).status(OrderStatus.RECEBIDA).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.EM_EXECUCAO);

        OrderResponse response = orderService.updateStatus(1L, request);

        assertEquals(OrderStatus.EM_EXECUCAO, response.getStatus());
        verify(orderEventRepository).save(any(OrderEvent.class));
        verify(eventPublisher).publish(any(OrderEvent.class));
    }

    @Test
    @DisplayName("updateStatus para FINALIZADA deve preencher data de finalizacao")
    void updateStatus_finalizada_devePreencherDataFim() {
        Order order = Order.builder().id(1L).status(OrderStatus.EM_EXECUCAO).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.FINALIZADA);

        OrderResponse response = orderService.updateStatus(1L, request);

        assertEquals(OrderStatus.FINALIZADA, response.getStatus());
        assertNotNull(response.getFinishedAt());
    }

    @Test
    @DisplayName("updateStatus nao deve permitir alterar OS entregue")
    void updateStatus_entregue_deveLancarExcecao() {
        Order order = Order.builder().id(1L).status(OrderStatus.ENTREGUE).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.EM_EXECUCAO);

        assertThrows(IllegalStateException.class, () -> orderService.updateStatus(1L, request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatus para o mesmo status deve lancar excecao")
    void updateStatus_mesmoStatus_deveLancarExcecao() {
        Order order = Order.builder().id(1L).status(OrderStatus.RECEBIDA).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.RECEBIDA);

        assertThrows(IllegalArgumentException.class, () -> orderService.updateStatus(1L, request));
    }

    @Test
    @DisplayName("getHistory deve retornar eventos da OS")
    void getHistory_deveRetornarEventos() {
        Order order = Order.builder().id(1L).status(OrderStatus.RECEBIDA).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderEvent event = OrderEvent.builder()
                .id("evt1")
                .orderId(1L)
                .eventType("ORDER_CREATED")
                .newStatus(OrderStatus.RECEBIDA)
                .occurredAt(LocalDateTime.now())
                .build();
        when(orderEventRepository.findByOrderIdOrderByOccurredAtAsc(1L))
                .thenReturn(List.of(event));

        List<OrderEventResponse> history = orderService.getHistory(1L);

        assertEquals(1, history.size());
        assertEquals("ORDER_CREATED", history.get(0).getEventType());
    }

    @Test
    @DisplayName("getHistory de OS inexistente deve lancar excecao")
    void getHistory_osInexistente_deveLancarExcecao() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.getHistory(99L));
    }
}

