package com.tech.challenger.execution.service.os.presentation.controller;

import com.tech.challenger.execution.service.os.application.dto.CreateOrderRequest;
import com.tech.challenger.execution.service.os.application.dto.OrderEventResponse;
import com.tech.challenger.execution.service.os.application.dto.OrderResponse;
import com.tech.challenger.execution.service.os.application.dto.UpdateOrderStatusRequest;
import com.tech.challenger.execution.service.os.application.service.OrderServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Ordens de Servico", description = "APIs de gerenciamento de Ordens de Servico (OS)")
public class OrderServiceController {

    private final OrderServiceImpl orderService;

    @PostMapping
    @Operation(summary = "Criar OS", description = "Cria/abre uma nova Ordem de Servico")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar OS", description = "Consulta uma Ordem de Servico pelo id")
    public ResponseEntity<OrderResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Atualizar status", description = "Atualiza o status de uma Ordem de Servico")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(id, request));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Historico", description = "Consulta o historico/auditoria de uma Ordem de Servico")
    public ResponseEntity<List<OrderEventResponse>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getHistory(id));
    }
}

