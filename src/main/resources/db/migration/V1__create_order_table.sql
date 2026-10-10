CREATE TABLE IF NOT EXISTS tb_order (
    id_order       BIGSERIAL PRIMARY KEY,
    id_cliente     BIGINT       NOT NULL,
    id_veiculo     BIGINT       NOT NULL,
    ds_status      VARCHAR(40)  NOT NULL,
    ds_descricao   VARCHAR(1000),
    dt_criacao     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dt_atualizacao TIMESTAMP,
    dt_finalizacao TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_order_status ON tb_order (ds_status);
CREATE INDEX IF NOT EXISTS idx_order_cliente ON tb_order (id_cliente);

