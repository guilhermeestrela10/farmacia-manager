(() => {
  let receitaAtual = null;
  let campos = [];
  function atualizarTotal() {
    const total = campos.reduce((soma, {item, campo}) => soma + Math.round(Number(item.preco) * 100) * (Number(campo.value) || 0), 0);
    $('total').textContent = 'Total: ' + moeda(total / 100);
  }
  function carregarReceita() {
    const id = $('receita').value;
    receitaAtual = null; campos = []; $('itens').replaceChildren(); $('resumo-receita').textContent = ''; atualizarTotal();
    return R.carregar('receita', () => id ? requisitar('/receitas/' + encodeURIComponent(id)) : Promise.resolve(null), receita => {
      if (!receita) return;
      receitaAtual = receita;
      $('resumo-receita').textContent = receita.clienteNome + ' · Receita ' + receita.numero + ' · Emissão: ' + dataBR(receita.emitidaEm) + ' · Validade: ' + dataBR(receita.validadeAte);
      for (const item of receita.itens.filter(i => i.restante > 0)) {
        const linha = criar('li');
        const campo = criar('input'); campo.type = 'number'; campo.min = '0'; campo.max = String(item.restante); campo.step = '1'; campo.value = '0'; campo.required = true;
        campo.id = 'quantidade-' + item.medicamentoId;
        const label = criar('label', '', 'Quantidade de ' + item.medicamentoNome); label.htmlFor = campo.id;
        linha.append(criar('span', '', item.medicamentoNome + ' · Restante: ' + item.restante + ' · Preço unitário: ' + moeda(item.preco)), label, campo);
        campo.addEventListener('input', atualizarTotal); campos.push({item, campo}); $('itens').append(linha);
      }
      if (!campos.length) $('itens').append(criar('li', 'vazio', 'Esta receita não possui saldo para dispensação.'));
    });
  }
  function carregarHistorico() {
    return R.carregar('dispensacoes', () => requisitar('/dispensacoes'), lista => {
      R.tabela('corpo', lista.map(d => [d.id, d.receitaId, d.clienteNome, d.vendaId, moeda(d.total), d.statusVenda, R.data(d.criadaEm)]));
    });
  }
  R.formulario('formulario', async () => {
    if (!receitaAtual) throw new Error('Selecione uma receita e aguarde seus itens.');
    const itens = [];
    for (const {item, campo} of campos) {
      const quantidade = Number(campo.value);
      if (!Number.isInteger(quantidade) || quantidade < 0 || quantidade > item.restante) throw new Error('Informe quantidades inteiras dentro do saldo restante.');
      if (quantidade > 0) itens.push({medicamentoId: item.medicamentoId, quantidade, precoEsperado: item.preco});
    }
    if (!itens.length) throw new Error('Informe a quantidade de pelo menos um medicamento.');
    const resposta = await enviar('POST', '/dispensacoes', {receitaId: receitaAtual.id, formaPagamento: $('pagamento').value, itens});
    R.aviso('Dispensação registrada. Venda nº ' + resposta.vendaId + ' · ' + moeda(resposta.total) + '.');
    await Promise.all([carregarReceita(), carregarHistorico()]);
  });
  $('receita').addEventListener('change', carregarReceita);
  $('atualizar-receita').addEventListener('click', carregarReceita);
  R.carregar('opcoes', () => requisitar('/receitas'), lista => {
    R.selecionar('receita', lista, r => r.numero + ' · ' + r.clienteNome + ' · até ' + dataBR(r.validadeAte));
  });
  carregarHistorico();
})();
