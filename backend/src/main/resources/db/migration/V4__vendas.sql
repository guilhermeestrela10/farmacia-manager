CREATE TABLE venda (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    vendedor_id          BIGINT        NOT NULL,
    forma_pagamento      VARCHAR(15)   NOT NULL,
    total                NUMERIC(12,2) NOT NULL,
    status               VARCHAR(15)   NOT NULL,
    criada_em            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    cancelada_em         TIMESTAMPTZ,
    cancelada_por_id     BIGINT,
    motivo_cancelamento  VARCHAR(200),

    CONSTRAINT fk_venda_vendedor
        FOREIGN KEY (vendedor_id) REFERENCES usuario (id),
    CONSTRAINT fk_venda_cancelada_por
        FOREIGN KEY (cancelada_por_id) REFERENCES usuario (id),
    CONSTRAINT ck_venda_forma
        CHECK (forma_pagamento IN ('DINHEIRO', 'PIX', 'DEBITO', 'CREDITO')),
    CONSTRAINT ck_venda_status
        CHECK (status IN ('CONCLUIDA', 'CANCELADA')),
    CONSTRAINT ck_venda_total CHECK (total > 0)
);

CREATE INDEX idx_venda_criada_em ON venda (criada_em);
CREATE INDEX idx_venda_vendedor ON venda (vendedor_id);

CREATE TABLE venda_item (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    venda_id        BIGINT        NOT NULL,
    medicamento_id  BIGINT        NOT NULL,
    quantidade      INTEGER       NOT NULL,
    preco_unitario  NUMERIC(10,2) NOT NULL,

    CONSTRAINT fk_venda_item_venda
        FOREIGN KEY (venda_id) REFERENCES venda (id),
    CONSTRAINT fk_venda_item_medicamento
        FOREIGN KEY (medicamento_id) REFERENCES medicamento (id),
    CONSTRAINT ck_venda_item_quantidade CHECK (quantidade > 0),
    CONSTRAINT ck_venda_item_preco CHECK (preco_unitario > 0)
);

CREATE INDEX idx_venda_item_venda ON venda_item (venda_id);
CREATE INDEX idx_venda_item_medicamento ON venda_item (medicamento_id);

ALTER TABLE movimentacao_estoque ADD COLUMN venda_id BIGINT;

ALTER TABLE movimentacao_estoque
    ADD CONSTRAINT fk_movimentacao_venda
    FOREIGN KEY (venda_id) REFERENCES venda (id);

CREATE INDEX idx_movimentacao_venda ON movimentacao_estoque (venda_id);