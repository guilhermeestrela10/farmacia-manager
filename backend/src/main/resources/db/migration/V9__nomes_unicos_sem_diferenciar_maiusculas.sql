CREATE UNIQUE INDEX uk_categoria_nome_sem_caixa ON categoria (lower(nome));
CREATE UNIQUE INDEX uk_fabricante_nome_sem_caixa ON fabricante (lower(nome));
