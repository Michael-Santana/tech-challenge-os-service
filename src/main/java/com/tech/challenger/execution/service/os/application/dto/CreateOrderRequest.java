package com.tech.challenger.execution.service.os.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateOrderRequest {

    @NotNull(message = "O id do cliente e obrigatorio")
    private Long customerId;

    @NotNull(message = "O id do veiculo e obrigatorio")
    private Long vehicleId;

    @Size(max = 1000, message = "A descricao deve ter no maximo 1000 caracteres")
    private String description;
}

