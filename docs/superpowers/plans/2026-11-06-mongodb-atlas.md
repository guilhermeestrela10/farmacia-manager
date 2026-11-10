# MongoDB Atlas Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans task-by-task.

**Goal:** Migrar a demonstração para o Atlas exclusivo, preservando os contratos e a consistência de estoque/vendas.

**Architecture:** Spring Data MongoDB, documentos por agregado, referências entre entidades, transações do replica set e conflitos de gravação tratados com rollback. Preservar IDs numéricos e valores Decimal128. Nenhuma alteração no cluster antigo.

**Tech Stack:** Java21, Spring Boot4.1.1, Spring Data MongoDB, MongoDB8 replica set, frontend vanilla, Render free.

**Spec:** docs/superpowers/specs/2026-11-06-mongodb-atlas-design.md

## Global Constraints

- Atlas permitido: org 6aed3a8735853522dc1e223e / projeto 6aed3bf85bae000716da9f23 / cluster 6aed3c10bf7422c83b745622 / host cluster0.x8sdnsm.mongodb.net / banco farmacia-manager. É outro Cluster0, exclusivo e vazio.
- Atlas proibido: projeto 6ae326b4c5d2b7aaf8f73045 / host cluster0.7zvdifp.mongodb.net.
- Render somente workspace tea-dbmg686k1f9s7385mci0, recursos free. Nunca My Workspace.
- Não remover dados nem origem PostgreSQL. Não expor segredos. Não editar migrações SQL históricas.
- Testes usam replica set local isolado; nunca bancos de outros sistemas.

## Review Focus

- Gravações explícitas substituem dirty checking; transação não pode perder atualização silenciosamente.
- Venda e auditoria, estorno e saldo, dispensação e receita devem confirmar juntos.
- Índices únicos, último ADMIN, conflitos concorrentes e dinheiro decimal mantêm suas garantias.
- Relatórios e históricos precisam consultar as mesmas referências persistidas, com snapshot consistente.
- Importação nunca sobrescreve destino existente; publicação somente após reconciliação e testes.

### Task 1: Documentos e repositórios (agent persistence)

- [ ] Converter entidades: @Document com nomes das tabelas atuais, @Id Long, @Version; BigDecimal em Decimal128; referências DBRef entre agregados; itens embutidos sem ciclos para pai.
- [ ] Substituir todos os repositórios JPA por Mongo, preservando métodos consumidos e aliases saveAndFlush/flush/getReferenceById para compatibilidade de testes. Implementar consultas, projeções e filtros no banco; agregações podem materializar apenas os candidatos filtrados para enriquecer referências.
- [ ] Gerar IDs com contador atômico por coleção, criar índices únicos antes de transações. Métodos ComTravamento devem fazer escrita de guarda na transação (findAndModify), impedindo que validações concorrentes confirmem sobre versões antigas.
- [ ] Fornecer setup Mongo e transações com snapshot/majority. Agregados devem ter gravação explícita pelos serviços. Não editar serviços.

### Task 2: Serviços de negócio (agent business)

- [ ] Converter Criteria/Specification para consultas MongoTemplate/Criteria filtradas e ordenadas, incluindo contagens/limites.
- [ ] Gravar explicitamente todas as entidades mutadas; preservar FEFO, preço confirmado, cancelamento, último admin, sessões, saldo prescrito e auditoria. Substituir isolamento SQL por transações Mongo snapshot; conflitos devem reverter, nunca ficar parciais.
- [ ] Manter assinaturas públicas e contratos DTO. Não editar modelos, repositórios, seed ou controllers.

### Task 3: Validação (agent tests)

- [ ] Adaptar testes SQL/JPA para replica set Mongo local isolado e profiles test/test,demo; preservar asserções úteis, trocar SQL de fixtures por MongoTemplate, nunca simplesmente desativar testes.
- [ ] Adaptar testar.ps1 para mongod isolado em porta55433 e replica set farmacia-teste, sem dependência de Docker ou Atlas. Download oficial do mongod somente se necessário; conferir documentação corrente. Root integra dependências/config.
- [ ] Testar concorrência, rollback, auditoria, preço decimal, ids, índices, cancelamento e dispensação. Não rodar Maven enquanto agentes alteram arquivos; coordenar com root.

### Task 4: Integração e publicação (root)

- [ ] Substituir dependências/configuração, seed/health, mapear conflitos HTTP; proteger todo rollback.
- [ ] Transferir somente dados desta demo, preservar IDs e referências, reconciliar origem/destino antes do corte. Validar credenciais exclusivas sem imprimir seus valores.
- [ ] Rodar suíte completa, revisar diff e publicar branch/PR. Configurar serviço gratuito candidato, validar HTTPS e banco Atlas efetivo antes do corte.
- [ ] Atualizar documentação e registrar limitações sem alegar prontidão para operação real.

## Estado em 09/11/2026

O backend MongoDB, o importador e os testes foram implementados. A suíte local com replica set descartável passou com 76 testes Java e 44 JavaScript. A versão pública ainda usa PostgreSQL.

O projeto Atlas dedicado tem a função `farmacia-manager-runtime`. Ela concede leitura e inclusão no banco `farmacia-manager`, com atualização restrita a `usuario`, `medicamento`, `lote`, `venda`, `cliente`, `receita`, `configuracao`, `_sequences` e `_guards`. A auditoria só recebe leitura e inclusão. Nenhum usuário recebeu essa função até este ponto.

O conector Atlas exigiu confirmação para liberar `74.220.49.0/24` e `74.220.57.0/24`, faixas exibidas em Connect no serviço Render da farmácia. A chamada não aplicou a mudança. Confira a lista de acesso antes de repetir.

O Blueprint `render-atlas.yaml` prepara um serviço candidato gratuito, com a carga fictícia automática desligada. Configure `MONGODB_URI` como segredo do serviço. O provisionamento inicial precisa de uma credencial temporária com `readWrite` e `dbAdmin` no banco exclusivo. Depois de validar o serviço e suspender as gravações na origem, habilite `APP_IMPORTAR_POSTGRES=true`. Confira o marcador de importação e a reconciliação antes do corte.

O Render planejou o Blueprint `exs-dbp7hr8m7kps73a31k30` no workspace dedicado. O plano cria somente o serviço gratuito `farmacia-manager-atlas` e aguarda o segredo `MONGODB_URI`. Ainda não foi aplicado. O modo de manutenção do Render exige um plano pago. Durante a transferência, use uma suspensão reversível do serviço PostgreSQL, após verificar a saúde do candidato, e preserve o banco de origem para eventual recuperação.

Após a transferência, desligue a importação, defina `APP_MONGO_PROVISIONAR=false`, use uma credencial dedicada com `farmacia-manager-runtime` e retire os segredos temporários. Valide a troca em HTTPS e publique o endereço verificado. Preserve a origem PostgreSQL para recuperação.
