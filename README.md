# Farmácia Manager

Sistema web de gerenciamento de farmácia com medicamentos, estoque por lote e validade, vendas, relatórios, usuários e auditoria. Backend Java 21 com Spring Boot 4.1.1, PostgreSQL 18 e frontend HTML/CSS/JavaScript servido pelo Spring.

## Funcionalidades disponíveis

- Categorias, fabricantes e medicamentos; edição e desativação preservando histórico.
- Estoque por lote, entrada, saída FEFO (primeiro a vencer), ajustes, perdas e alertas.
- Vendas, carrinho, pagamento, troco, recibo e leitor de código de barras via Enter.
- Histórico e totais por pagamento; cancelamento administrativo com estorno aos lotes originais.
- Relatórios de vendas e posição do estoque, com exportação CSV.
- Perfis ADMIN/VENDEDOR, bloqueio de tentativas, troca/redefinição de senha e encerramento de sessões antigas.
- Auditoria de acessos, alterações, recusas e preços; registros protegidos contra edição e exclusão.

Clientes, atendimento farmacêutico, receitas, dispensações, configurações e uma tela dedicada de lotes continuam planejados. Os lotes já podem ser consultados e movimentados na tela Estoque. Medicamentos controlados e emissão de NFC-e estão fora do escopo atual.

## Executar no Windows

Requisitos: JDK 21 ou superior, PostgreSQL 18 e PowerShell. O Maven Wrapper está incluído. Neste ambiente, a pasta do projeto é `C:\Users\guilh\projetos\farmacia-manager`.

Crie o banco `farmacia_db` pelo pgAdmin. Prepare a configuração local somente se ela ainda não existir:

```powershell
cd 'C:\Users\guilh\projetos\farmacia-manager\backend'
if (-not (Test-Path 'src/main/resources/application-local.properties')) {
    Copy-Item 'src/main/resources/application-local.properties.example' 'src/main/resources/application-local.properties'
}
```

Edite `backend/src/main/resources/application-local.properties` localmente: conexão do banco e dados do administrador inicial. Esse arquivo é ignorado pelo Git. Nunca publique senhas. O administrador inicial só é criado quando a tabela de usuários está vazia; a senha deve ter de 8 a 72 caracteres e até 72 bytes em UTF-8.

Inicie a aplicação (ajuste JAVA_HOME se seu JDK estiver em outra pasta):

```powershell
cd 'C:\Users\guilh\projetos\farmacia-manager\backend'
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.5'
.\mvnw.cmd spring-boot:run
```

Abra [a página de login](http://localhost:8080/login.html). O Flyway aplica as migrações pendentes ao iniciar; Hibernate valida o esquema. Não execute manualmente `docs/modelo-banco.sql`: ele registra o desenho inicial.

Pare com Ctrl+C. Após editar Java ou arquivos estáticos, reinicie e atualize o navegador com Ctrl+F5. Se a porta 8080 estiver ocupada, encerre a instância antiga antes de iniciar outra.

## Testes

O comando abaixo executa os testes JavaScript, cria um PostgreSQL descartável com configuração regional portuguesa na porta 55432 e roda toda a suíte Java. Depois encerra e remove esse banco. Precisa de Node.js e PostgreSQL 18 instalado em `C:\Program Files\PostgreSQL\18\bin`.

```powershell
cd 'C:\Users\guilh\projetos\farmacia-manager\backend'
.\testar.ps1
```

O script não reutiliza bancos existentes, não lê a configuração local e recusa execução se a porta de teste estiver ocupada. Os testes de integração usam somente o perfil `test` e o usuário fictício `farmacia_teste`.

Para compilar sem iniciar a aplicação:

```powershell
cd 'C:\Users\guilh\projetos\farmacia-manager\backend'
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.5'
.\mvnw.cmd -q -DskipTests compile
```

O último ciclo completo passou com 41 testes Java e 6 testes JavaScript. Inclui FEFO, venda tudo-ou-nada, cancelamento, concorrência de estoque e usuários, permissões, senhas e migrações V1–V9.

## Regras importantes

- Saldo nunca é alterado sem movimentação. Lotes vencidos só são baixados por perda.
- Preço de venda vem do servidor. A tela envia também o preço confirmado para recusar alterações ocorridas depois da inclusão no carrinho.
- Vendedor não acessa Gestão. Cancelamento exige administrador e motivo.
- A versão da senha autenticada é conferida a cada requisição; redefinir a senha encerra sessões antigas na próxima requisição.
- Migrações já aplicadas nunca são editadas. V3 e V5 foram aplicadas vazias neste projeto; V8 completa sua estrutura sem alterar checksums.
- V9 garante unicidade de nomes de categorias e fabricantes sem distinguir maiúsculas, conforme a configuração regional do PostgreSQL. Duplicatas existentes devem ser resolvidas antes de aplicar essa versão; a migração não apaga nem mescla cadastros.

O bloqueio de login ainda fica em memória e é limpo ao reiniciar. A busca do catálogo ainda filtra em Java. HTTPS, backups e configuração de produção continuam pendentes. Use dados fictícios na validação.

Veja o [escopo atual](docs/escopo-mvp.md), o [diagnóstico da retomada](docs/retomada-seguranca-4b-4c.md) e o [registro das correções](docs/correcoes-revisao-projeto.md).
