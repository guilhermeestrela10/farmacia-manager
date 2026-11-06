# Demonstração completa e publicação

O usuário confirmou demonstração com dados fictícios, somente recursos gratuitos e uso exclusivo do workspace Render `farmacia-manager` (`tea-dbmg686k1f9s7385mci0`). O workspace `My Workspace` é proibido e não foi modificado. A evolução para uso real, controles regulatórios e NFC-e é futura.

## Entrega

Preservar Java 21/Spring Boot/PostgreSQL e HTML/CSS/JS. Manter a revisão de segurança da branch atual. Completar o menu com clientes, lotes/validades, atendimentos manuais, registros de receitas de exemplo, dispensações vinculadas a vendas e configuração básica da farmácia. Exibir permanentemente que o ambiente é de demonstração; não gerar prescrições, recomendações clínicas ou documentos fiscais.

Backend e frontend continuam no mesmo serviço. Banco PostgreSQL separado, novo e gratuito no workspace autorizado. Somente dados fictícios; não copiar configurações, contas ou bancos locais. Perfis `prod,demo` para a publicação. O modo demo oferece botões de acesso por perfil, sem divulgar ou fixar senhas. Esses endpoints só existem no profile demo.

## Contratos de API

Todas as rotas abaixo exigem sessão, salvo `/demo/info`, `/demo/entrar` (POST com CSRF; somente profile demo) e `/health`.

- GET `/clientes?busca`: lista de `{id,nome,telefone,email,ativo}`. POST `/clientes`: `{nome,telefone,email}`; PATCH `/clientes/{id}/ativar` ou `/desativar`. Nome 1–100, telefone até30, email validado até150. Nunca excluir.
- GET `/atendimentos?clienteId`: lista `{id,clienteId,clienteNome,profissional,observacoes,criadoEm}`. POST mesmo sem id/nome/data: `{clienteId,profissional,observacoes}`. Profissional 1–100, observações 1–2000. Registro manual imutável; cliente deve existir e estar ativo.
- GET `/receitas?clienteId` e GET `/receitas/{id}`: `{id,clienteId,clienteNome,prescritor,numero,emitidaEm,validadeAte,observacoes,itens}`. Item: `{medicamentoId,medicamentoNome,preco,quantidade,dispensado,restante}`. POST `/receitas`: `{clienteId,prescritor,numero,emitidaEm,validadeAte,observacoes,itens:[{medicamentoId,quantidade}]}`. Datas coerentes, emissão não futura; 1–20 itens únicos, quantidade1–9999, medicamento ativo. Observações opcionais até2000; prescritor1–100; número1–80.
- GET `/dispensacoes`: lista `{id,receitaId,clienteNome,vendaId,total,statusVenda,criadaEm}`. POST `/dispensacoes`: `{receitaId,formaPagamento,itens:[{medicamentoId,quantidade,precoEsperado}]}`. Trava receita, valida período, cliente ativo, itens prescritos e saldo ainda não dispensado. Reutiliza VendaService para preço/FEFO/estoque e confirma tudo na mesma transação. Quantidade dispensada considera somente vendas CONCLUIDAS; cancelamentos administrativos já existentes devolvem o saldo prescrito e o estoque.
- GET `/configuracoes`: `{nomeFarmacia,telefone,email,endereco}`. PUT `/gestao/configuracoes`: mesmos campos; ADMIN. Nome1–100, telefone até30, email até150, endereço até200. Configuração singleton criada pela migração com identidade Farmácias Ruanna.
- GET `/estoque/lotes?busca&situacao`: LoteResponse existente, apenas saldo positivo, ordenado por validade/id. Situação `TODOS`, `VALIDO`, `PROXIMO` (até90dias), `VENCIDO`; filtro inválido400. Busca por medicamento/lote sem diferenciar caixa.

Erros seguem ApiExceptionHandler e status existentes. Novas tabelas somente em V10; V1–V9 não serão alteradas. Novas mutações passam pela auditoria transacional existente. Não armazenar senhas, corpos ou tokens em auditoria.

## Interface

Páginas `clientes.html`, `lotes.html`, `atendimento.html`, `receitas.html`, `dispensacoes.html`, `gestao-configuracoes.html`, todas com layout/padrões existentes, textContent, proteção contra duplo envio, erros claros e respostas antigas descartadas. Menu com links reais, sem itens "em breve" dentro deste escopo. Receitas e atendimento são exemplos manuais sem validade clínica/fiscal. Dispensa mostra itens restantes e preço, forma de pagamento e número da venda. Configuração altera dados cadastrais e nome exibido no layout.

## Seed e publicação

Seed apenas no profile demo e em banco exclusivo. Contas `demo.admin` e `demo.vendedor` com hashes de senhas aleatórias não divulgadas; acesso controlado por endpoints exclusivos do modo demo. Medicamentos não controlados, categorias, fabricantes, lotes válidos/vencidos, clientes, atendimentos, receitas e vendas fictícios; seed idempotente. Sem aconselhamento de dosagem.

Docker build Java21; runtime não root, porta por PORT, timezone America/Sao_Paulo. Profile prod lê DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD do ambiente, cookie secure e cabeçalhos de proxy. Docker ignora configurações locais/segredos. `/health` testa disponibilidade do banco sem expor detalhes.

Validar testes Java/JS em PostgreSQL descartável, fluxos principais em navegador com conta de demo, deploy até estado live e HTTPS real. Só recursos free; não alterar planos ou recursos de outro sistema. Informar no final o link e os limites de suspensão por ociosidade e expiração do banco gratuito.
