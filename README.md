# 💊 Farmácia Manager

> Sistema de gerenciamento de farmácia desenvolvido com foco em organização, controle de estoque, atendimento farmacêutico, gerenciamento de receitas e dispensação de medicamentos.

[![Status](https://img.shields.io/badge/status-em%20desenvolvimento-yellow)]()
[![Java](https://img.shields.io/badge/Java-Backend-orange)]()
[![Spring%20Boot](https://img.shields.io/badge/Spring%20Boot-Backend-brightgreen)]()
[![MySQL](https://img.shields.io/badge/MySQL-Database-blue)]()
[![Git](https://img.shields.io/badge/Git-Version%20Control-orange)]()
[![License](https://img.shields.io/badge/license-TBD-lightgrey)]()

---

## 📋 Sobre o projeto

O **Farmácia Manager** é um projeto de software desenvolvido com o objetivo de criar uma plataforma completa para gerenciamento de operações de uma farmácia.

A proposta é centralizar diferentes processos em um único sistema, permitindo o gerenciamento de:

- 💊 Medicamentos
- 📦 Estoque
- 🏷️ Lotes
- 📅 Validades
- 🔄 Movimentações de estoque
- 👥 Clientes
- 🧑‍⚕️ Atendimento farmacêutico
- 📋 Receitas
- 💉 Dispensação de medicamentos
- 👤 Funcionários e usuários
- 🔐 Permissões de acesso
- 📊 Dashboard
- 📈 Relatórios
- 📝 Auditoria e histórico de operações

O projeto está sendo desenvolvido de forma **incremental**, começando por funcionalidades fundamentais e evoluindo gradualmente para uma solução mais completa.

---

# 🎯 Objetivo

O principal objetivo do projeto é desenvolver uma aplicação que possa auxiliar uma farmácia no gerenciamento de suas operações internas, reduzindo processos manuais e centralizando informações importantes.

Além do objetivo técnico e educacional, o projeto possui uma possível finalidade comercial futura.

A intenção é desenvolver inicialmente um protótipo funcional, validá-lo com profissionais da área farmacêutica e, a partir do feedback obtido, evoluir o sistema.

### Fluxo planejado

```text
                    FARMÁCIA
                       │
          ┌────────────┼────────────┐
          │            │            │
          ▼            ▼            ▼
       ESTOQUE     ATENDIMENTO   RECEITAS
          │            │            │
          │            │            ▼
          │            │       DISPENSAÇÃO
          │            │            │
          └────────────┴────────────┘
                       │
                       ▼
                HISTÓRICO / RELATÓRIOS
