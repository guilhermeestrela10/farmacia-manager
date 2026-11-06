(() => {
  let medicamentos = [];
  let itens = [];
  function renderItens() {
    $('itens').replaceChildren();
    for (const item of itens) {
      const linha = criar('li');
      linha.append(criar('span', '', item.nome + ' · ' + item.quantidade + ' un.'), R.botao('Remover ' + item.nome, () => {
        itens = itens.filter(i => i.medicamentoId !== item.medicamentoId); renderItens();
      }));
      $('itens').append(linha);
    }
  }
  function carregar() {
    const cliente = $('filtro-cliente').value;
    return R.carregar('receitas', () => requisitar('/receitas' + (cliente ? '?clienteId=' + encodeURIComponent(cliente) : '')), lista => {
      R.tabela('corpo', lista.map(item => [item.numero, item.clienteNome, item.prescritor, dataBR(item.emitidaEm), dataBR(item.validadeAte), R.botao('Ver receita ' + item.numero, () => detalhe(item.id))]));
    });
  }
  function detalhe(id) {
    return R.carregar('detalhe', () => requisitar('/receitas/' + id), receita => {
      $('titulo-detalhe').textContent = 'Receita ' + receita.numero;
      $('resumo-detalhe').textContent = receita.clienteNome + ' · ' + receita.prescritor + ' · Emissão: ' + dataBR(receita.emitidaEm) + ' · Validade: ' + dataBR(receita.validadeAte);
      $('observacoes-detalhe').textContent = receita.observacoes || 'Sem observações.';
      R.tabela('itens-detalhe', receita.itens.map(i => [i.medicamentoNome, i.quantidade, i.dispensado, i.restante, moeda(i.preco)]));
      if (!$('detalhe').open) $('detalhe').showModal();
    });
  }
  $('adicionar').addEventListener('click', () => {
    limparErro();
    const medicamento = medicamentos.find(m => m.id === Number($('medicamento').value));
    const quantidade = Number($('quantidade').value);
    if (!medicamento || !Number.isInteger(quantidade) || quantidade < 1 || quantidade > 9999) {
      mostrarErro('Selecione um medicamento e uma quantidade inteira de 1 a 9999.'); return;
    }
    if (itens.length >= 20) {mostrarErro('A receita aceita até 20 medicamentos.'); return;}
    if (itens.some(i => i.medicamentoId === medicamento.id)) {mostrarErro('Medicamento já adicionado. Remova o item para alterar sua quantidade.'); return;}
    itens.push({medicamentoId: medicamento.id, nome: medicamento.nome, quantidade});
    renderItens();
  });
  R.formulario('formulario', async () => {
    if (!itens.length) throw new Error('Adicione pelo menos um medicamento.');
    const emitidaEm = $('emitida-em').value;
    const validadeAte = $('validade-ate').value;
    if (emitidaEm > new Date().toLocaleDateString('sv-SE') || validadeAte < emitidaEm) throw new Error('Informe datas coerentes; a emissão não pode estar no futuro.');
    await enviar('POST', '/receitas', {clienteId: Number($('cliente').value), prescritor: $('prescritor').value.trim(), numero: $('numero').value.trim(), emitidaEm, validadeAte, observacoes: $('observacoes').value.trim(), itens: itens.map(({medicamentoId, quantidade}) => ({medicamentoId, quantidade}))});
    $('formulario').reset(); itens = []; renderItens(); R.aviso('Receita de exemplo registrada.'); await carregar();
  });
  $('fechar-detalhe').addEventListener('click', () => $('detalhe').close());
  $('filtro-cliente').addEventListener('change', carregar);
  $('emitida-em').max = new Date().toLocaleDateString('sv-SE');
  R.carregar('opcoes', () => Promise.all([requisitar('/clientes'), requisitar('/medicamentos')]), ([clientes, meds]) => {
    medicamentos = meds.filter(m => m.ativo !== false);
    R.selecionar('cliente', clientes.filter(c => c.ativo), c => c.nome);
    R.selecionar('filtro-cliente', clientes, c => c.nome, 'Todos os clientes');
    R.selecionar('medicamento', medicamentos, m => m.nome);
  });
  carregar();
})();
