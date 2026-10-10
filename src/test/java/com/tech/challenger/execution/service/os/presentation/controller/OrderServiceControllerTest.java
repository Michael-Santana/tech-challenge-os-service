package com.tech.challenger.execution.service.os.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tech.challenger.execution.service.os.application.dto.CreateOrderRequest;
import com.tech.challenger.execution.service.os.application.dto.OrderEventResponse;
import com.tech.challenger.execution.service.os.application.dto.OrderResponse;
import com.tech.challenger.execution.service.os.application.dto.UpdateOrderStatusRequest;
import com.tech.challenger.execution.service.os.application.exception.ResourceNotFoundException;
import com.tech.challenger.execution.service.os.application.service.OrderServiceImpl;
import com.tech.challenger.execution.service.os.domain.model.OrderStatus;
import com.tech.challenger.execution.service.os.presentation.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderServiceController.class)
@Import(GlobalExceptionHandler.class)
class OrderServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderServiceImpl orderService;

    @Test
    @DisplayName("POST /orders deve criar OS e retornar 201")
    void create_deveRetornar201() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        request.setVehicleId(2L);
        request.setDescription("Troca de oleo");

        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .customerId(1L)
                .vehicleId(2L)
                .status(OrderStatus.RECEBIDA)
                .description("Troca de oleo")
                .createdAt(LocalDateTime.now())
                .build();

        when(orderService.create(any(CreateOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("RECEBIDA")));
    }

    @Test
    @DisplayName("POST /orders sem customerId deve retornar 400")
    void create_semCustomerId_deveRetornar400() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setVehicleId(2L);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /orders/{id} deve retornar OS")
    void findById_deveRetornar200() throws Exception {
        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .customerId(1L)
                .vehicleId(2L)
                .status(OrderStatus.RECEBIDA)
                .build();

        when(orderService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    @DisplayName("GET /orders/{id} inexistente deve retornar 404")
    void findById_inexistente_deveRetornar404() throws Exception {
        when(orderService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Ordem de servico nao encontrada: 99"));

        mockMvc.perform(get("/orders/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /orders/{id}/status deve atualizar status")
    void updateStatus_deveRetornar200() throws Exception {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.EM_EXECUCAO);

        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .status(OrderStatus.EM_EXECUCAO)
                .build();

        when(orderService.updateStatus(eq(1L), any(UpdateOrderStatusRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("EM_EXECUCAO")));
    }

    @Test
    @DisplayName("GET /orders/{id}/history deve retornar historico")
    void getHistory_deveRetornar200() throws Exception {
        OrderEventResponse event = OrderEventResponse.builder()
                .id("abc123")
                .orderId(1L)
                .eventType("ORDER_CREATED")
                .newStatus(OrderStatus.RECEBIDA)
                .occurredAt(LocalDateTime.now())
                .build();

        when(orderService.getHistory(1L)).thenReturn(List.of(event));

        mockMvc.perform(get("/orders/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].eventType", is("ORDER_CREATED")));
    }
}

