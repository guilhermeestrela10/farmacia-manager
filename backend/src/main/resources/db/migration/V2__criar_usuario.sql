CREATE TABLE usuario (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome        VARCHAR(100)  NOT NULL,
    login       VARCHAR(50)   NOT NULL,
    senha_hash  VARCHAR(100)  NOT NULL,
    perfil      VARCHAR(20)   NOT NULL,
    ativo       BOOLEAN       NOT NULL DEFAULT TRUE,
    criado_em   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_usuario_login UNIQUE (login),
    CONSTRAINT ck_usuario_perfil CHECK (perfil IN ('ADMIN', 'VENDEDOR')),
    CONSTRAINT ck_usuario_login_minusculo CHECK (login = LOWER(login))
);