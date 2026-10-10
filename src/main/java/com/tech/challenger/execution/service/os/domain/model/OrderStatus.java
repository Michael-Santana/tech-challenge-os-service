package com.tech.challenger.execution.service.os.domain.model;

/**
 * Status possiveis de uma Ordem de Servico (OS).
 */
public enum OrderStatus {
    RECEBIDA,
    EM_DIAGNOSTICO,
    AGUARDANDO_APROVACAO,
    EM_EXECUCAO,
    FINALIZADA,
    ENTREGUE,
    CANCELADA
}

