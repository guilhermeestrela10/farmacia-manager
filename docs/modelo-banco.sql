-- ============================================================
-- farmacia-manager | Modelo do banco (versão 0.1)
-- SGBD: PostgreSQL
-- Execute este script já conectado ao banco "farmacia_db".
-- ============================================================

-- ------------------------------------------------------------
-- CATEGORIA
-- ------------------------------------------------------------
CREATE TABLE categoria (
    id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome  VARCHAR(80) NOT NULL,
    CONSTRAINT uk_categoria_nome UNIQUE (nome)
);

-- ------------------------------------------------------------
-- FABRICANTE
-- ------------------------------------------------------------
CREATE TABLE fabricante (
    id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome  VARCHAR(120) NOT NULL,
    CONSTRAINT uk_fabricante_nome UNIQUE (nome)
);

-- ------------------------------------------------------------
-- MEDICAMENTO
-- ------------------------------------------------------------
CREATE TABLE medicamento (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome            VARCHAR(150)   NOT NULL,
    codigo_barras   VARCHAR(20),
    categoria_id    BIGINT         NOT NULL,
    fabricante_id   BIGINT         NOT NULL,
    preco           NUMERIC(10,2)  NOT NULL,
    estoque_minimo  INTEGER        NOT NULL DEFAULT 0,
    ativo           BOOLEAN        NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_medicamento_codigo_barras UNIQUE (codigo_barras),
    CONSTRAINT fk_medicamento_categoria
        FOREIGN KEY (categoria_id)  REFERENCES categoria (id),
    CONSTRAINT fk_medicamento_fabricante
        FOREIGN KEY (fabricante_id) REFERENCES fabricante (id),
    CONSTRAINT ck_medicamento_preco          CHECK (preco > 0),
    CONSTRAINT ck_medicamento_estoque_minimo CHECK (estoque_minimo >= 0)
);

CREATE INDEX idx_medicamento_nome ON medicamento (nome);

-- ------------------------------------------------------------
-- LOTE
-- ------------------------------------------------------------
CREATE TABLE lote (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    medicamento_id    BIGINT       NOT NULL,
    numero_lote       VARCHAR(50)  NOT NULL,
    validade          DATE         NOT NULL,
    quantidade_atual  INTEGER      NOT NULL DEFAULT 0,
    criado_em         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_lote_medicamento
        FOREIGN KEY (medicamento_id) REFERENCES medicamento (id),
    CONSTRAINT uk_lote_medicamento_numero UNIQUE (medicamento_id, numero_lote),
    CONSTRAINT ck_lote_quantidade CHECK (quantidade_atual >= 0)
);

CREATE INDEX idx_lote_medicamento ON lote (medicamento_id);
CREATE INDEX idx_lote_validade    ON lote (validade);

-- ------------------------------------------------------------
-- MOVIMENTACAO_ESTOQUE
-- ------------------------------------------------------------
CREATE TABLE movimentacao_estoque (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    lote_id      BIGINT        NOT NULL,
    tipo         VARCHAR(20)   NOT NULL,
    quantidade   INTEGER       NOT NULL,
    motivo       VARCHAR(200),
    responsavel  VARCHAR(100)  NOT NULL,
    data_hora    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_movimentacao_lote
        FOREIGN KEY (lote_id) REFERENCES lote (id),
    CONSTRAINT ck_movimentacao_tipo
        CHECK (tipo IN ('ENTRADA', 'SAIDA', 'AJUSTE_ENTRADA', 'AJUSTE_SAIDA', 'PERDA')),
    CONSTRAINT ck_movimentacao_quantidade CHECK (quantidade > 0)
);

CREATE INDEX idx_movimentacao_lote_data
    ON movimentacao_estoque (lote_id, data_hora);
