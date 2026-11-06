# Migração para MongoDB Atlas

## Objetivo e situação

Usar MongoDB Atlas como banco do Farmácia Manager, mantendo APIs, telas e regras existentes. O usuário indicou o nome `farmacia-manager`. A aplicação online permanece no PostgreSQL até a troca estar validada.

Na inspeção inicial, o conector Atlas encontrou somente `Project 0` / `Cluster0` (MongoDB 8.0, plano gratuito). O banco `farmacia-manager` não aparece entre os bancos persistidos. Falta confirmar se esse é o destino pretendido ou se o usuário criou outro projeto/cluster. Não criar, copiar ou alterar dados no cluster compartilhado antes dessa identificação. Não solicitar senha no chat.

## Alcance confirmado no código

- Substituir JPA/Hibernate, consultas JPQL/Criteria e a configuração JDBC/Flyway do runtime por Spring Data MongoDB.
- Adaptar 14 repositórios e seus consumidores: catálogo, usuários, lotes, movimentações, vendas/itens, clientes, atendimentos, receitas/itens, dispensações, configuração e auditoria.
- IDs numéricos continuam no contrato HTTP e no frontend; a geração deve ser atômica. Valores monetários permanecem decimais exatos, sem conversão para ponto flutuante.
- Itens de vendas e receitas pertencem ao documento pai. Referências mutáveis devem ser resolvidas de modo a preservar edição de medicamentos, usuários e configuração sem duplicar estados desatualizados.
- MongoDB não oferece dirty checking de JPA: todas as mutações hoje implícitas precisam de gravação explícita.
- Locks pessimistas não podem ser simplesmente removidos. Vendas, estornos, dispensações e a proteção do último administrador precisam de concorrência equivalente, conflitos controlados e transações reais.
- Usar MongoTransactionManager e opções de leitura/escrita compatíveis com o replica set. Estoque, venda/dispensação e auditoria devem confirmar juntos ou reverter juntos. Recusas continuam auditadas separadamente.
- Criar índices e validação dos documentos antes de transações de negócio; preservar unicidade de login, nomes normalizados e identificação medicamento/lote. Não editar migrações SQL já aplicadas.
- Adaptar health check, carga demo idempotente, testes de integração, Docker, documentação e Render. Não alterar o frontend sem incompatibilidade confirmada.

## Dados e implantação

O destino precisa de usuário exclusivo com acesso somente ao banco `farmacia-manager`. Não reutilizar credenciais de outros sistemas. Guardar conexão apenas no ambiente seguro do serviço Render, nunca em Git, saída de testes ou documentação.

Inventariar origem e destino, definir transferência idempotente e validar contagens, IDs, totais, saldos por lote e integridade das referências. Não sobrescrever coleções existentes, apagar a origem ou reiniciar dados silenciosamente. Se o destino não estiver vazio, interromper a importação e apresentar o conflito.

Publicar em branch `codex/`, exclusivamente no workspace Render `farmacia-manager`, com plano gratuito. Testar a aplicação candidata antes de substituir o serviço saudável. Não fazer merge automático na main ou alterar My Workspace. Desativar dependência de PostgreSQL somente após conferência funcional e reconciliação bem-sucedidas.

## Validação de aceite

Testes em MongoDB replica set isolado: venda concorrente sem saldo negativo, rollback de múltiplos itens, FEFO, cancelamento concorrente sem duplo estorno, limite de receita com dispensações concorrentes, cancelamento liberando saldo prescrito, último administrador, unicidade, preços exatos, sessões/CSRF/perfis e atomicidade da auditoria. Relatórios precisam de leitura consistente durante alterações.

Manter testes JavaScript e verificar por HTTPS login de ambos os perfis, cadastros, venda, dispensação, cancelamento e relatórios. Registrar a versão implantada e confirmar que o banco efetivo é Atlas antes de anunciar a migração concluída.
