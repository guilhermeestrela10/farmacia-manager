-- V3 e V5 foram aplicadas vazias; manter seus checksums e reparar instalações novas.
ALTER TABLE medicamento ADD COLUMN IF NOT EXISTS tipo VARCHAR(15) NOT NULL DEFAULT 'REFERENCIA';
CREATE INDEX IF NOT EXISTS idx_medicamento_categoria ON medicamento (categoria_id);

ALTER TABLE movimentacao_estoque DROP CONSTRAINT ck_movimentacao_tipo;
ALTER TABLE movimentacao_estoque ADD CONSTRAINT ck_movimentacao_tipo
    CHECK (tipo IN ('ENTRADA', 'SAIDA', 'AJUSTE_ENTRADA', 'AJUSTE_SAIDA', 'PERDA', 'ESTORNO'));
