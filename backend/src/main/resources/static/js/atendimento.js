(() => {
  function carregar() {
    const cliente = $('filtro-cliente').value;
    return R.carregar('atendimentos', () => requisitar('/atendimentos' + (cliente ? '?clienteId=' + encodeURIComponent(cliente) : '')), lista => {
      R.tabela('corpo', lista.map(item => [item.clienteNome, item.profissional, item.observacoes, R.data(item.criadoEm)]));
    });
  }
  R.formulario('formulario', async () => {
    await enviar('POST', '/atendimentos', {clienteId: Number($('cliente').value), profissional: $('profissional').value.trim(), observacoes: $('observacoes').value.trim()});
    $('formulario').reset(); R.aviso('Atendimento manual registrado.'); await carregar();
  });
  $('filtro-cliente').addEventListener('change', carregar);
  R.carregar('clientes', () => requisitar('/clientes'), lista => {
    R.selecionar('cliente', lista.filter(c => c.ativo), c => c.nome);
    R.selecionar('filtro-cliente', lista, c => c.nome, 'Todos os clientes');
  });
  carregar();
})();
