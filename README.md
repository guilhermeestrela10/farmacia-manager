# 💊 Farmácia Manager

### Sistema de gestão inteligente para farmácias

<p align="center">
  <strong>Organização • Controle • Segurança • Informação</strong>
</p>

<p align="center">
  Um projeto em desenvolvimento com o objetivo de centralizar processos importantes da rotina de uma farmácia em uma única plataforma.
</p>

---

## 📌 Sobre o projeto

O **Farmácia Manager** é um projeto de sistema de gestão desenvolvido com o objetivo de auxiliar na organização e no controle das atividades realizadas diariamente em uma farmácia.

A proposta é construir uma plataforma que reúna diferentes áreas da operação em um único ambiente, permitindo que informações importantes sejam registradas, consultadas e acompanhadas de maneira mais organizada.

O projeto começou inicialmente pelo **controle de estoque**, mas foi planejado para crescer gradualmente e futuramente integrar outras áreas importantes, como:

- 📦 Gestão de estoque
- 💊 Cadastro de medicamentos
- 🏷️ Controle de lotes
- 📅 Controle de validade
- 🔄 Movimentações de estoque
- 👥 Cadastro de clientes
- 🧑‍⚕️ Atendimento farmacêutico
- 📋 Controle de receitas
- 💉 Registro de dispensações
- 📊 Dashboard gerencial
- 📈 Relatórios
- 👤 Usuários e permissões
- 🔐 Segurança e auditoria

O sistema está sendo desenvolvido de forma incremental, começando por uma base sólida e evoluindo conforme novas necessidades são identificadas.

---

# 🎯 Objetivo

O principal objetivo do Farmácia Manager é **facilitar a organização das informações e dos processos da farmácia**.

A ideia não é simplesmente criar várias telas e funcionalidades.

O objetivo é construir um sistema no qual os diferentes setores possam trabalhar de maneira integrada.

Por exemplo:

```text
                    ┌──────────────────┐
                    │     FARMÁCIA     │
                    └────────┬─────────┘
                             │
                             ▼
                  ┌─────────────────────┐
                  │   FARMÁCIA MANAGER  │
                  └──────────┬──────────┘
                             │
          ┌──────────────────┼──────────────────┐
          │                  │                  │
          ▼                  ▼                  ▼
     📦 ESTOQUE        🧑‍⚕️ ATENDIMENTO     📋 RECEITAS
          │                  │                  │
          ▼                  ▼                  ▼
       LOTES             HISTÓRICO          DISPENSAÇÃO
          │                                     │
          ▼                                     ▼
      VALIDADES                         MOVIMENTAÇÃO
          │                                     │
          └─────────────────┬───────────────────┘
                            ▼
                    📊 INFORMAÇÕES
                     E RELATÓRIOS
