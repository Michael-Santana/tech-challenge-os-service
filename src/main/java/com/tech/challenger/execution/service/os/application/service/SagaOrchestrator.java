package com.tech.challenger.execution.service.os.application.service;

import com.tech.challenger.execution.service.os.domain.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Orquestrador de Sagas do OS Service.
 * Coordena os passos distribuidos da criacao/atualizacao de uma OS
 * (validacao de cliente/veiculo, reserva de pecas no estoque, orcamento, etc.).
 *
 * Nesta fase, os passos sao executados de forma simplificada (log + evento),
 * podendo evoluir para chamadas remotas/orquestracao via mensageria.
 */
@Slf4j
@Component
public class SagaOrchestrator {

    /**
     * Inicia a saga de criacao de uma Ordem de Servico.
     *
     * Passos previstos:
     * 1. Validar cliente (Customer Service)
     * 2. Validar veiculo (Vehicle Service)
     * 3. Reservar itens de estoque (Inventory Service)
     * 4. Gerar orcamento (Budget Service)
     */
    public void startOrderCreationSaga(Order order) {
        log.info("Iniciando saga de criacao da OS {}", order.getId());
        // TODO: publicar comandos para os demais microsservicos e tratar compensacoes
        log.info("Saga de criacao da OS {} iniciada com sucesso", order.getId());
    }
}

