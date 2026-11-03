# Correções da revisão — 03/11/2026

A revisão considerou uma farmácia com vários operadores e abas simultâneas. Priorizou dinheiro, estoque, permissões, sessões e preservação de dados; não adicionou módulos de produto ou dependências.

## Corrigido

| Problema reproduzido | Correção | Validação |
| --- | --- | --- |
| Etapas de auditoria e senhas incompletas impediam compilar | Restaurados arquivos faltantes e conectados filtros, serviços e tela | Compilação, testes de login, auditoria e senhas |
| Reset podia sobrescrever desativação e restaurar estado antigo | Todas as mutações do usuário leem a linha com trava de escrita | Reset e desativação simultâneos com PostgreSQL |
| Dois administradores podiam deixar zero ativos | Administradores bloqueados em ordem antes da decisão | Duas transações concorrentes; resta um ativo e a outra recebe 409 |
| Administrador inicial inválido podia derrubar a inicialização | Validados nome, login, caracteres e bytes da senha antes do BCrypt | Testes de limites e configuração inválida |
| Duas entradas disputavam a criação do mesmo lote | Trava no medicamento antes de consultar/criar o lote | Entradas simultâneas somam 12 unidades em um lote |
| Preço ou ativação podiam mudar durante a venda | Cadastro e venda compartilham a trava do medicamento | Contexto JPA completo e preço divergente |
| Carrinho antigo podia confirmar venda acima do valor recebido | `precoEsperado` opcional por item é conferido contra o cadastro; a tela sempre o envia | Recusa 409 antes de gravar; carrinho preservado para reconfirmação |
| Carrinho podia ser alterado durante envio | Ações de inclusão, remoção, quantidade e limpeza bloqueadas durante a requisição | Testes JavaScript com requisição pendente |
| Respostas antigas exibiam lotes de outro medicamento | Estado limpo e ações bloqueadas durante carga; respostas antigas ignoradas | Testes A→B→A, inclusive respostas fora de ordem |
| Dois envios de formulário duplicavam cadastros/operações | Trava de envio e botão desabilitado até a resposta | Clique repetido e nova tentativa após erro |
| Soma de lotes grandes podia estourar inteiro | Soma em `long`; adição ao lote detecta overflow | Dois lotes de Integer.MAX_VALUE |
| Ajuste de saída podia baixar vencido | Baixa recusada; perda continua permitida | Ajuste vencido não altera saldo/histórico |
| Total grande demais falhava no banco com 500 | Limite validado antes do insert | Sem gravação de venda ou baixa |
| Item nulo chegava ao serviço | Validação de cada elemento e cascata de seus campos | Bean Validation |
| Cadastros concorrentes podiam duplicar nomes por caixa | V9 adiciona índices únicos em lower(nome) | Banco português, com nomes acentuados |
| Violação de unicidade escapava sem mensagem de negócio | Resposta 409 sem detalhes SQL; overflow retorna 400 | Testes do handler |
| Cancelamento não definia ordem dos bloqueios | Movimentações ordenadas por validade, lote e id | Reposição aos lotes de origem |
| Teste de contexto podia acessar configuração local | Perfil `test`, conexão fictícia e habilitação explícita | Suíte sem usar farmacia_db ou loja |

## Resultado

Passaram 41 testes Java e 6 testes JavaScript. A suíte iniciou o contexto completo do Spring, validou o esquema Hibernate, aplicou V1–V9 via Flyway e verificou FEFO, vencidos, cancelamento repetido, venda tudo-ou-nada e disputas reais entre transações. O runner `backend/testar.ps1` cria e remove um PostgreSQL isolado com locale ICU pt-BR.

O `farmacia_db` real foi consultado somente para histórico/estrutura e contagem de duplicatas; `loja` não foi acessado. Não havia grupos duplicados de categorias/fabricantes na consulta. Migrações novas continuam pendentes no banco real até iniciar a aplicação.

## Limites

Não foi feita validação visual no navegador nem demonstração com a farmacêutica. Não houve implantação ou alteração no banco real. Busca integral do catálogo em Java e bloqueio de login em memória permanecem como limites conhecidos. HTTPS, backup/restauração, NFC-e e novos módulos não foram implementados.

Os testes usam configuração regional portuguesa. A semântica de `lower(nome)` acompanha a configuração regional do PostgreSQL; V9 não altera collation nem normaliza nomes sem acentos. A migração recusa duplicatas existentes sem apagar ou mesclar cadastros.

As mudanças foram publicadas na branch `codex/corrigir-seguranca-4b-4c`; a integração na `main` é uma etapa separada.
