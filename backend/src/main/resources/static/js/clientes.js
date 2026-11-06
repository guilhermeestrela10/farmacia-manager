(() => {
  function carregar() {
    return R.carregar('clientes', () => requisitar('/clientes?busca=' + encodeURIComponent($('busca').value.trim())), lista => {
      R.tabela('corpo', lista.map(cliente => [cliente.nome, cliente.telefone, cliente.email,
        cliente.ativo ? 'Ativo' : 'Inativo', R.botao(cliente.ativo ? 'Desativar' : 'Ativar', async () => {
          await enviar('PATCH', '/clientes/' + cliente.id + (cliente.ativo ? '/desativar' : '/ativar'));
          R.aviso('Situação do cliente atualizada.'); await carregar();
        })]));
    });
  }
  R.formulario('formulario', async () => {
    await enviar('POST', '/clientes', {nome: $('nome').value.trim(), telefone: $('telefone').value.trim(), email: $('email').value.trim()});
    $('formulario').reset(); R.aviso('Cliente cadastrado.'); await carregar();
  });
  $('busca').addEventListener('input', carregar);
  carregar();
})();
