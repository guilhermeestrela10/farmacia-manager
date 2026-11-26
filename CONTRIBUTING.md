# Contribuição

## Preparar uma alteração

Leia o README e o escopo atual. Parta da branch `main` atualizada, crie uma branch para a mudança e mantenha o diff focado no problema descrito. Preserve o histórico e não faça force-push na branch compartilhada.

Use um ambiente próprio, dados fictícios e arquivos locais de configuração ignorados pelo Git. Nunca use o banco publicado para testar alterações.

## Validar

Na pasta `backend/`, execute `.\testar.ps1` no Windows. O roteiro usa PostgreSQL descartável; requisitos e opções de compilação estão no README.

Execute as verificações relevantes, revise o diff e informe no pull request o que foi validado e o que ficou pendente. Se mudar comportamento visual, confira teclado, movimento reduzido quando aplicável e larguras de celular e computador.

Mudanças de banco devem usar novas migrações; não edite migrações já aplicadas. Explique impacto, compatibilidade e estratégia de recuperação quando houver alteração de dados.

## Reportar problemas

Inclua comportamento esperado, passos de reprodução, ambiente e versão ou commit. Use os formulários de issue para problemas funcionais e propostas de melhoria.

Não publique vulnerabilidades exploráveis ou credenciais em issues. Siga [SECURITY.md](SECURITY.md).
