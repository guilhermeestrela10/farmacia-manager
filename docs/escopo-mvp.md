# Escopo do MVP (Versão 0.1)

> Documento vivo. Será atualizado conforme o projeto evoluir e conforme o feedback dos farmacêuticos.

## 1. Objetivo da versão 0.1

Entregar um sistema web pequeno e funcional que controle **medicamentos e estoque**, com lote e validade, para ser demonstrado a farmacêuticos e validar a base do projeto.

## 2. Decisões já tomadas

| Tema | Decisão |
|---|---|
| Banco de dados | PostgreSQL (administração via pgAdmin 4) |
| Backend | Java 21 + Spring Boot 3 |
| Persistência | Spring Data JPA + Flyway (migrações) |
| Frontend | HTML, CSS e JavaScript puro |
| Arquitetura | Monólito em camadas (controller, service, repository) |
| Medicamentos controlados (tarja preta) | **Fora do escopo** do projeto |
| Dados | Apenas dados fictícios durante o desenvolvimento |

## 3. Faz parte do MVP

### Medicamentos
- [ ] Cadastrar medicamento
- [ ] Listar medicamentos
- [ ] Pesquisar por nome
- [ ] Editar medicamento
- [ ] Desativar medicamento (não excluir)
- [ ] Categorias e fabricantes (cadastro simples)

### Estoque
- [ ] Registrar **entrada** (com lote e validade)
- [ ] Registrar **saída**
- [ ] Consultar quantidade disponível (total e por lote)
- [ ] Definir estoque mínimo por medicamento
- [ ] Histórico de movimentações

### Alertas básicos
- [ ] Estoque baixo
- [ ] Sem estoque
- [ ] Lotes próximos do vencimento

### Interface
- [ ] Telas simples para as funcionalidades acima
- [ ] Mensagens de erro claras

## 4. Fica fora do MVP

- Clientes, atendimento, receitas e dispensação (versões 0.2 a 0.5)
- Login, usuários e permissões (versão 0.6)
- Dashboard completo e gráficos (versão 0.7)
- Relatórios (versão 0.8)
- Auditoria completa, backup automático e logs avançados (versão 0.9)
- Venda/caixa
- Múltiplas filiais
- Leitor de código de barras
- Hospedagem

> **Observação:** a movimentação de estoque já terá um campo de "responsável", preenchido de forma provisória, para evitar refazer a tabela quando o login for implementado.

## 5. Regras de negócio iniciais

1. Nome, preço (maior que zero) e estoque mínimo (maior ou igual a zero) são obrigatórios.
2. Um medicamento pode ter vários lotes, cada um com sua validade.
3. O número do lote é único dentro do mesmo medicamento.
4. O estoque nunca fica negativo.
5. Toda alteração de saldo gera uma movimentação registrada.
6. Movimentações não são editadas nem apagadas; erros são corrigidos com um **ajuste**.
7. Medicamento com histórico é **desativado**, nunca excluído.
8. Lote vencido não pode ter saída para dispensação.
9. Alerta de estoque baixo: saldo total menor ou igual ao estoque mínimo.
10. Alerta de vencimento: lote que vence em até N dias (valor inicial sugerido: 90, configurável).
11. Saída consome primeiro o lote de validade mais próxima (FEFO). **A validar com farmacêuticos.**

## 6. Modelo inicial do banco (rascunho)

```
categoria (id, nome)
fabricante (id, nome)
medicamento (id, nome, codigo_barras, categoria_id, fabricante_id,
             preco, estoque_minimo, ativo, criado_em)
lote (id, medicamento_id, numero_lote, validade, quantidade_atual, criado_em)
movimentacao_estoque (id, lote_id, tipo, quantidade, motivo,
                      responsavel, data_hora, observacao)
```

Tipos de movimentação: `ENTRADA`, `SAIDA`, `AJUSTE`, `PERDA`.

## 7. Dúvidas em aberto (para responder ou levar aos farmacêuticos)

- [ ] O estoque é controlado por caixa, cartela ou unidade? Existe fracionamento?
- [ ] O preço é único ou precisa de histórico?
- [ ] Como funciona a baixa por lote na prática? O FEFO é seguido?
- [ ] Qual antecedência de alerta de vencimento é útil (30, 60, 90 dias)?
- [ ] O código de barras é usado no dia a dia?
- [ ] Quais informações são consultadas com mais frequência?
- [ ] Quais tarefas são mais repetitivas hoje?

## 8. Critério de "pronto" da versão 0.1

A versão 0.1 está pronta quando for possível, com dados fictícios:

1. Cadastrar um medicamento.
2. Dar entrada de dois lotes com validades diferentes.
3. Dar saída e ver o saldo atualizado.
4. Ver o alerta de estoque baixo e de vencimento próximo.
5. Consultar o histórico de movimentações.
6. Tentar uma saída maior que o saldo e receber uma mensagem de erro clara.
