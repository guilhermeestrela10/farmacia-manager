const R = (() => {
  const versoes = new Map();
  const envios = new WeakSet();
  function aviso(texto) {
    $('aviso').textContent = texto;
    $('aviso').hidden = false;
  }
  async function carregar(chave, pedido, renderizar) {
    const versao = (versoes.get(chave) || 0) + 1;
    versoes.set(chave, versao);
    limparErro();
    try {
      const dados = await pedido();
      if (versoes.get(chave) === versao) renderizar(dados);
    } catch (erro) {
      if (versoes.get(chave) === versao) mostrarErro(erro.message);
    }
  }
  function tabela(id, linhas) {
    const corpo = $(id);
    corpo.replaceChildren();
    for (const valores of linhas) {
      const tr = criar('tr');
      for (const valor of valores) {
        const td = criar('td');
        if (valor && typeof valor === 'object') td.append(valor);
        else td.textContent = valor ?? '—';
        tr.append(td);
      }
      corpo.append(tr);
    }
    if (!linhas.length) {
      const td = criar('td', 'vazio', 'Nenhum registro encontrado.');
      td.colSpan = 20;
      const tr = criar('tr'); tr.append(td); corpo.append(tr);
    }
  }
  function selecionar(id, lista, rotulo, vazio = 'Selecione...') {
    const select = $(id);
    select.replaceChildren();
    const opcao = criar('option', '', vazio); opcao.value = ''; select.append(opcao);
    for (const item of lista) {
      const option = criar('option', '', rotulo(item)); option.value = String(item.id); select.append(option);
    }
  }
  async function executar(elemento, acao) {
    if (envios.has(elemento)) return;
    envios.add(elemento);
    const anterior = elemento.disabled;
    elemento.disabled = true;
    limparErro();
    try { await acao(); }
    catch (erro) { mostrarErro(erro.message); }
    finally { elemento.disabled = anterior; envios.delete(elemento); }
  }
  function formulario(id, acao) {
    const form = $(id);
    form.addEventListener('submit', evento => {
      evento.preventDefault();
      return executar(form.querySelector('fieldset'), acao);
    });
  }
  function botao(texto, acao) {
    const b = criar('button', 'btn sec', texto); b.type = 'button';
    b.addEventListener('click', () => executar(b, acao)); return b;
  }
  function data(valor) { return valor ? new Date(valor).toLocaleString('pt-BR') : '—'; }
  return { aviso, carregar, tabela, selecionar, executar, formulario, botao, data };
})();
