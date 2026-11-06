# Demonstração completa Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Concluir a demonstração funcional com dados fictícios e entregar URL HTTPS verificada.

**Architecture:** Monólito Spring existente com novos registros ligados ao estoque/vendas. Frontend vanilla mantém o layout e os helpers atuais. Deploy Docker em serviço e PostgreSQL gratuitos, exclusivamente no workspace autorizado.

**Tech Stack:** Java21, SpringBoot4.1.1, PostgreSQL18, Flyway, HTML/CSS/JS, Render.

**Spec:** `docs/superpowers/specs/2026-11-05-demo-completa-design.md`.

## Global Constraints

- Somente workspace `tea-dbmg686k1f9s7385mci0`; nunca My Workspace.
- Somente planos free; sem banco local ou dados reais.
- Não alterar V1–V9, não adicionar dependências por conveniência.
- Publicação autorizada pelo usuário; manter código em branch codex até revisão.
- Sem senhas em código, chat, logs ou auditoria; endpoints demo só no profile demo.

## Review Focus

- Dispensas concorrentes nunca ultrapassam receita nem estoque.
- Cancelamento libera unidades prescritas e devolve lote correto.
- Receitas vencidas, clientes inativos e valores inválidos são recusados.
- Duplo clique, erro de rede e resposta antiga não corrompem formulários.
- Modo demo nunca habilita acesso público em perfil normal; configurações locais nunca entram na imagem.

### Task 1: Backend dos módulos

- [ ] Criar migração V10, entidades, DTOs, repositórios, serviços e controllers dos contratos.
- [ ] Reutilizar VendaService e locks existentes para dispensações.
- [ ] Criar testes de validação, concorrência, cancelamento e configuração ADMIN.
- [ ] Conferir contrato e testar em PostgreSQL descartável.

### Task 2: Interface completa

- [ ] Criar seis páginas e JS com layout, menu real e aviso de demonstração.
- [ ] Implementar formulários/listas, receitas por itens e dispensação de saldo restante.
- [ ] Testar envios, erros e rendering sem innerHTML de dados.
- [ ] Integrar nome/configuração da farmácia no layout.

### Task 3: Seed, acesso demo e implantação

- [ ] Criar seed idempotente e acesso demo por perfil sem senha fixa.
- [ ] Criar profile prod, health e Dockerfile/.dockerignore.
- [ ] Validar Java/JS e navegação local com banco isolado.
- [ ] Criar PostgreSQL free e serviço Docker free no workspace autorizado.
- [ ] Publicar branch, acompanhar deploy live, verificar login, páginas e HTTPS.
- [ ] Entregar URL e limites do plano gratuito; registrar resultado.
