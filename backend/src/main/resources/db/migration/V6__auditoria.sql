CREATE TABLE auditoria (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    data_hora TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('ACESSO', 'ALTERACAO', 'PRECO')),
    usuario VARCHAR(50) NOT NULL,
    acao VARCHAR(500) NOT NULL,
    metodo VARCHAR(10) NOT NULL,
    caminho VARCHAR(200) NOT NULL,
    status INTEGER NOT NULL CHECK (status BETWEEN 100 AND 599),
    ip VARCHAR(45) NOT NULL
);

CREATE INDEX idx_auditoria_data ON auditoria (data_hora DESC, id DESC);
CREATE INDEX idx_auditoria_usuario ON auditoria (usuario);

CREATE FUNCTION recusar_alteracao_auditoria() RETURNS TRIGGER
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Registros de auditoria são imutáveis.';
END;
$$;

CREATE TRIGGER auditoria_imutavel
BEFORE UPDATE OR DELETE OR TRUNCATE ON auditoria
FOR EACH STATEMENT EXECUTE FUNCTION recusar_alteracao_auditoria();
