package com.tech.challenger.execution.service.os.application.dto;

import com.tech.challenger.execution.service.os.domain.model.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateOrderStatusRequest {

    @NotNull(message = "O novo status e obrigatorio")
    private OrderStatus status;

    private String description;
}

