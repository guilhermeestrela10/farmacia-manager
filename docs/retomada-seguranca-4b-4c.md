# Retomada das etapas 4B e 4C — 03/11/2026

Pasta: `C:\Users\guilh\projetos\farmacia-manager`.
Branch de trabalho: `codex/corrigir-seguranca-4b-4c`.
Este documento registra o diagnóstico inicial. As alterações locais anteriores foram preservadas.
Depois, o usuário autorizou commits e push; o resultado atualizado está em `correcoes-revisao-projeto.md`.
A branch `wip-seguranca-4b-4c` não estava disponível localmente nem no GitHub.

## Diagnóstico inicial

O primeiro comando parou com `The JAVA_HOME environment variable is not defined correctly`.
O JDK disponível é `C:\Program Files\Java\jdk-25.0.5`; a configuração foi ajustada somente nos processos de terminal usados nesta tarefa.

Com esse JDK, apareceram 7 erros `cannot find symbol`:

- `UsuarioService.java`: importação, campo e construtor de `TentativasDeLogin` (linhas 9, 27 e 31).
- `SecurityConfig.java`: importação e parâmetro de `AuditoriaService` (linhas 4 e 33), parâmetro de `TentativasDeLogin` (linha 32) e construção de `AuditoriaFilter` (linha 99).

As linhas acima descrevem os arquivos antes das correções. A compilação não reproduziu os erros relatados anteriormente em RelatorioService, VendaService, EstoqueService ou AlertaService.

Arquivos previstos no contexto que estavam ausentes, agora criados:

- `backend/src/main/java/com/farmacia/farmacia/model/TipoAuditoria.java`
- `backend/src/main/java/com/farmacia/farmacia/model/RegistroAuditoria.java`
- `backend/src/main/java/com/farmacia/farmacia/dto/AuditoriaResponse.java`
- `backend/src/main/java/com/farmacia/farmacia/repository/AuditoriaRepository.java`
- `backend/src/main/java/com/farmacia/farmacia/service/AuditoriaService.java`
- `backend/src/main/java/com/farmacia/farmacia/controller/AuditoriaController.java`
- `backend/src/main/java/com/farmacia/farmacia/security/TentativasDeLogin.java`
- `backend/src/main/java/com/farmacia/farmacia/security/AuditoriaFilter.java`
- `backend/src/main/java/com/farmacia/farmacia/security/DescritorDeAcao.java`
- `backend/src/main/java/com/farmacia/farmacia/security/AuditoriaDeAcessos.java`
- `backend/src/main/resources/db/migration/V6__auditoria.sql`
- `backend/src/main/resources/static/gestao-auditoria.html`
- `backend/src/main/resources/static/js/gestao-auditoria.js`

Não foram encontrados arquivos `.jav`, `.java.txt` ou outros nomes de extensão incorreta no inventário verificado. Os únicos arquivos vazios encontrados em `src/main` foram `V3__medicamento_tipo.sql` e `V5__estorno_de_venda.sql`.

## Banco e migrações

A consulta ao banco real foi somente leitura, restrita ao `farmacia_db` local. O banco `loja` não foi acessado.

O histórico real contém V1–V5 aplicadas; V3 e V5 têm checksum `0`. A tabela de auditoria e a V6 ainda não existiam. A coluna `usuario.senha_alterada_em` ainda não existia. O check de movimentações já aceitava ESTORNO.

- V1–V5 foram preservadas, inclusive os arquivos vazios e seus checksums.
- V6 cria a auditoria com triggers que recusam UPDATE, DELETE e TRUNCATE.
- V7, já presente nas alterações locais, foi preservada.
- V8 completa instalações novas que passaram pelas V3/V5 vazias: adiciona `medicamento.tipo` quando ausente, cria o índice de categoria e permite ESTORNO. O tipo inicial de medicamentos sem essa coluna é REFERENCIA; medicamentos de um banco que já tem a coluna não são reclassificados.

V6–V8 ainda não foram aplicadas ao banco real. O Flyway aplicará essas versões quando a aplicação for iniciada.

## Correções

Auditoria de acessos e mutações, filtros administrativos, tela de consulta, bloqueio após 5 falhas por 15 minutos e auditoria de alteração de preço foram conectados aos fluxos existentes. A auditoria não lê corpo nem query string das requisições. A tela usa `textContent` para dados.

O bloqueio normaliza o login e impede autenticação com a senha correta enquanto estiver ativo. Continua em memória: reiniciar o processo limpa o contador, e múltiplas instâncias não compartilham tentativas.

A nova classe `security/UsuarioAutenticado.java` transporta a versão da senha lida junto do hash. A sessão guarda essa versão no atributo existente `loginEm`; `UsuarioAtivoFilter` compara igualdade com a versão atual do banco. Usar apenas o horário do login deixava uma corrida com redefinições concorrentes. Sessões com formato antigo deixam de valer.

A redefinição administrativa agora também rejeita a mesma senha. `MedicamentoRequest` aceita no máximo 8 dígitos inteiros e 2 casas decimais, evitando que o banco arredonde um valor diferente daquele registrado na auditoria. A alteração do preço e seu registro participam da mesma transação.

## Validação executada

17 testes passaram, sem falhas, erros ou testes ignorados:

| Classe | Testes |
| --- | ---: |
| TentativasDeLoginTest | 2 |
| AuditoriaFilterTest | 2 |
| SecurityConfigTest | 4 |
| UsuarioAtivoFilterTest | 3 |
| UsuarioServiceTest | 4 |
| MedicamentoPrecoTest | 1 |
| MigracoesSegurancaTest | 1 |

O teste de migrações utilizou PostgreSQL 18.3 descartável em `127.0.0.1:55432`, com usuário fictício `farmacia_teste`. Aplicou V1–V8 pelo Flyway em um schema novo, validou o histórico e verificou a imutabilidade da auditoria. Essa instância foi encerrada ao final.

Também passaram a verificação de sintaxe dos scripts alterados de auditoria, usuários e troca de senha e `git diff --check`. `application-local.properties` continua ignorado pelo Git e não faz parte da entrega.

O teste antigo `FarmaciaApplicationTests.contextLoads` não foi executado: ele inicia o contexto real e poderia aplicar migrações ao banco local. A aplicação completa e as telas ainda precisam de validação manual.

Para repetir os 16 testes sem banco, use:

```powershell
cd 'C:\Users\guilh\projetos\farmacia-manager\backend'
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.5'
.\mvnw.cmd -q '-Dtest=TentativasDeLoginTest,AuditoriaFilterTest,UsuarioServiceTest,SecurityConfigTest,UsuarioAtivoFilterTest,MedicamentoPrecoTest' test
```

O teste MigracoesSegurancaTest é separado e exige um PostgreSQL descartável na porta 55432 e a propriedade `farmacia.test.url`. Sem essa propriedade ele é ignorado. Não aponte esse teste para bancos de uso real.

O Graphify foi usado para mapear o código inicial, sem provedor externo ou chave de API. O grafo local está em `backend/src/main/java/graphify-out/`, excluído apenas por `.git/info/exclude`; não integra a entrega ou os commits.

## Próxima etapa: validar manualmente

Inicie a aplicação:

```powershell
cd 'C:\Users\guilh\projetos\farmacia-manager\backend'
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.5'
.\mvnw.cmd spring-boot:run
```

Abra `http://localhost:8080/login.html` e atualize com Ctrl+F5. A inicialização aplicará V6, V7 e V8 ao `farmacia_db` configurado. Se a porta 8080 estiver ocupada, encerre a instância antiga antes de tentar novamente.

1. Entre como administrador e abra Gestão → Auditoria. Confira login e logout com IP.
2. Com dados fictícios, registre entrada, ajuste, perda, venda, cancelamento e criação de usuário. Confira as descrições na auditoria.
3. Altere o preço de um medicamento e confira o registro de valor anterior → novo valor. Um preço com três casas decimais deve ser recusado pela API.
4. Entre como vendedor e tente acessar `/gestao/auditoria` e `/usuarios`: espere 403. Uma tentativa de mutação administrativa com CSRF válido deve aparecer como Recusado (403).
5. Teste 5 senhas erradas com vendedor1, nunca com o administrador. A quinta falha deve retornar 429; a senha correta também deve ser recusada durante o bloqueio. Reiniciar limpa o bloqueio em memória.
6. Abra duas sessões do mesmo vendedor. Troque a própria senha informando a atual. A sessão que fez a troca termina imediatamente; a outra termina na próxima requisição. Faça novo login com a senha nova.
7. Repita a redefinição pelo administrador e confira a queda da sessão do vendedor na próxima requisição. A mesma senha deve ser recusada.
8. Confira que nenhuma senha aparece na auditoria ou nas mensagens do sistema. Os testes de UPDATE/DELETE/TRUNCATE já foram executados no banco descartável; não é necessário repetir comandos destrutivos no banco real.

Depois da confirmação desses testes, revisar o diff e os arquivos incluídos antes de criar o commit. Não executar `git add .` sem conferir os arquivos; preservar a configuração local fora do Git.
