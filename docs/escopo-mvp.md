# Escopo do MVP — atualizado em 03/11/2026

O sistema atende ao cadastro de medicamentos, estoque e vendas de uma farmácia, com dados fictícios durante a validação. A identidade visual segue as Farmácias Ruanna.

## Implementado

- [x] Categorias e fabricantes com nomes únicos, sem diferenciar maiúsculas conforme a configuração regional do banco.
- [x] Medicamentos de referência, genéricos e similares; busca por nome, código, categoria, fabricante e tipo.
- [x] Edição e desativação de medicamentos, preservando o histórico.
- [x] Estoque por lote, validade e estoque mínimo; entrada, saída, ajuste, perda e histórico.
- [x] Baixa FEFO e bloqueio de venda/saída de lotes vencidos.
- [x] Alertas de estoque baixo, sem estoque, vencidos e próximos do vencimento.
- [x] Vendas, carrinho, pagamento, cálculo de troco, recibo e leitor de código de barras via Enter.
- [x] Histórico de vendas, totais por pagamento e cancelamento por administrador.
- [x] Relatórios de vendas, posição do estoque e exportação CSV. Não há cálculo de lucro porque não existe preço de custo.
- [x] Login, perfis ADMIN/VENDEDOR, administração de usuários e troca/redefinição de senha.
- [x] Auditoria de acessos, alterações e preços; bloqueio temporário de tentativas de login.
- [x] Dashboard com gráficos e menu por seções.
- [x] Testes automatizados de segurança, estoque, vendas, concorrência, migrações e estados assíncronos das telas.

## Regras preservadas

1. Toda alteração de saldo gera movimentação. Erros são corrigidos com ajustes; histórico não é apagado pela aplicação.
2. Medicamento é desativado, nunca excluído. Desativado não recebe entrada, saída ou venda.
3. Lote é único por medicamento e número; novas entradas devem manter sua validade.
4. Estoque disponível considera lotes dentro da validade; lotes vencidos só podem ser baixados por perda.
5. Venda usa o preço do cadastro, grava o valor no item e confirma todos os itens ou reverte tudo.
6. Se o preço mudou desde a confirmação na tela, a venda é recusada e o usuário precisa confirmar o novo valor.
7. Cancelamento exige ADMIN e motivo; só ocorre uma vez e devolve as unidades aos mesmos lotes.
8. Só vendas concluídas entram nos totais dos relatórios.
9. Vendedor acessa a operação e suas próprias vendas, sem acesso às rotas de Gestão.
10. Auditoria não registra senhas, corpo de formulário ou query string; o banco impede UPDATE, DELETE e TRUNCATE dos registros.
11. Alterações simultâneas de usuário não podem restaurar senhas antigas, reativar desativados ou eliminar o último administrador ativo.

## Fora do escopo atual

Medicamentos controlados (tarja preta), emissão de NFC-e, múltiplas filiais, clientes, atendimento farmacêutico, receitas e dispensações. As últimas telas continuam identificadas como planejadas no menu.

## Validações de produto ainda necessárias

- Regras de antibióticos e controle de receita.
- Necessidade de emissão fiscal.
- Preço de custo, margem e permissões para alterar preço ou corrigir estoque.
- Prazo de cancelamento de venda e de retenção da auditoria.
- Fracionamento e unidade de controle de estoque.
- Categorias desejadas e antecedência dos alertas de validade.
- Demonstração completa com a farmacêutica, usando dados fictícios.

## Base técnica

Java 21, Spring Boot 4.1.1, PostgreSQL 18, Spring Data JPA, Spring Security, Bean Validation e Flyway. Frontend HTML/CSS/JavaScript puro. O esquema vigente é definido por `backend/src/main/resources/db/migration`, atualmente V1–V9; `modelo-banco.sql` é uma referência histórica.

O bloqueio de login em memória, a busca em Java, a configuração de HTTPS/backup e a hospedagem continuam como limites e próximos trabalhos. Veja o README para executar a aplicação e a suíte completa.
