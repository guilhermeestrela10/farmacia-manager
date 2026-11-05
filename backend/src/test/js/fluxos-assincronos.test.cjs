const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

function pendente() {
  let resolve, reject;
  const promise = new Promise((sim, nao) => { resolve = sim; reject = nao; });
  return { promise, resolve, reject };
}

function relatorio(total = 0) {
  return { totalVendido: total, porDia: [], porForma: [], porVendedor: [],
    porCategoria: [], maisVendidos: [], de: '2026-11-01', ate: '2026-11-05' };
}

async function pagina(arquivo) {
  const elementos = new Map();
  const criar = (_tag, _classe, texto) => ({ value: '', hidden: false, disabled: false,
    textContent: texto, children: [], events: {}, open: false, fechamentos: 0,
    append(...itens) { this.children.push(...itens); },
    replaceChildren(...itens) { this.children = itens; },
    addEventListener(tipo, acao) { this.events[tipo] = acao; },
    showModal() { this.open = true; }, close() { this.open = false; this.fechamentos++; },
    setAttribute() {}, classList: { add() {} },
  });
  const $ = id => {
    if (!elementos.has(id)) elementos.set(id, criar());
    return elementos.get(id);
  };
  const csvs = [criar(), criar()];
  const contexto = vm.createContext({ $, criar, URLSearchParams, moeda: String, dataBR: String,
    window: { usuarioAtual: { login: 'admin' } }, location: { search: '', href: '' },
    document: { querySelectorAll: seletor => seletor === '[data-csv]' ? csvs : [],
      createElementNS: criar },
    limparErro() { $('erro').hidden = true; },
    mostrarErro(texto) { $('erro').textContent = texto; $('erro').hidden = false; },
    confirm: () => true, setTimeout() {}, clearTimeout() {},
    requisitar: async url => url === '/auth/me' ? { perfil: 'VENDEDOR' }
      : url.startsWith('/gestao/relatorios/') ? relatorio()
      : url.startsWith('/vendas?') ? { vendas: [], porForma: [] } : [],
  });
  vm.runInContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/js', arquivo), 'utf8'), contexto);
  await new Promise(setImmediate);
  const executar = codigo => vm.runInContext(codigo, contexto);
  const pedidos = [];
  contexto.requisitar = url => {
    const pedido = { url, ...pendente() };
    pedidos.push(pedido);
    return pedido.promise;
  };
  return { $, csvs, contexto, executar, pedidos };
}

for (const [arquivo, funcao, render, resposta] of [
  ['vendas.js', 'carregarProdutos', 'renderProdutos = () => $("resultado").textContent = produtos[0].id', id => [{ id }]],
  ['medicamentos.js', 'carregarLista', 'renderTabela = () => $("resultado").textContent = medicamentos[0].id', id => [{ id }]],
  ['vendas-historico.js', 'carregar', 'renderCartoes = () => {}; renderTabela = h => $("resultado").textContent = h.id', id => ({ id })],
  ['movimentacoes.js', 'carregar', 'renderTabela = h => $("resultado").textContent = h.id', id => ({ id })],
]) {
  test(`${arquivo}: somente última consulta altera lista e erro`, async () => {
    const t = await pagina(arquivo);
    t.executar(render);
    const antigas = [t.executar(`${funcao}()`), t.executar(`${funcao}()`)];
    const atual = t.executar(`${funcao}()`);
    t.pedidos[2].resolve(resposta(3));
    await atual;
    t.pedidos[0].resolve(resposta(1));
    t.pedidos[1].reject(new Error('Falha antiga'));
    await Promise.all(antigas);
    assert.equal(t.$('resultado').textContent, 3);
    assert.equal(t.$('erro').hidden, true);
  });
}

for (const [arquivo, funcao] of [['vendas.js', 'carregarProdutos'], ['medicamentos.js', 'carregarLista']]) {
  test(`${arquivo}: digitação invalida resposta antes de executar debounce`, async () => {
    const t = await pagina(arquivo);
    t.executar('renderProdutos = () => $("resultado").textContent = "antigo"; renderTabela = () => $("resultado").textContent = "antigo";');
    const antiga = t.executar(`${funcao}()`);
    t.$('busca').value = 'nova busca';
    t.$('busca').events.input();
    t.pedidos[0].resolve([]);
    await antiga;
    assert.notEqual(t.$('resultado').textContent, 'antigo');
  });
}

test('scanner não adiciona produto anterior quando a consulta falha', async () => {
  const t = await pagina('vendas.js');
  t.executar('produtos = [{id: 1, disponivel: 5}]; adicionar = p => $("adicionado").textContent = p.id;');
  t.$('busca').value = 'codigo B';
  const leitura = t.$('busca').events.keydown({ key: 'Enter', preventDefault() {} });
  t.pedidos[0].reject(new Error('Sem rede'));
  await new Promise(setImmediate);
  for (const pedido of t.pedidos.slice(1)) pedido.resolve([]);
  await leitura;
  assert.equal(t.$('adicionado').textContent, undefined);
  assert.equal(t.$('busca').value, 'codigo B');
});

test('scanner não adiciona resultado invalidado por nova digitação', async () => {
  const t = await pagina('vendas.js');
  t.executar('renderProdutos = () => {}; adicionar = p => $("adicionado").textContent = p.id;');
  const leitura = t.$('busca').events.keydown({ key: 'Enter', preventDefault() {} });
  t.$('busca').value = 'outro codigo';
  t.$('busca').events.input();
  t.pedidos[0].resolve([{ id: 1, disponivel: 5 }]);
  await new Promise(setImmediate);
  for (const pedido of t.pedidos.slice(1)) pedido.resolve([]);
  await leitura;
  assert.equal(t.$('adicionado').textContent, undefined);
});

test('scanner adiciona resultado fresco e limpa código somente após sucesso', async () => {
  const t = await pagina('vendas.js');
  t.executar('renderProdutos = () => {}; adicionar = p => $("adicionado").textContent = p.id;');
  t.$('busca').value = 'codigo A';
  const leitura = t.$('busca').events.keydown({ key: 'Enter', preventDefault() {} });
  t.pedidos[0].resolve([{ id: 1, disponivel: 5 }]);
  await new Promise(setImmediate);
  assert.equal(t.$('adicionado').textContent, 1);
  assert.equal(t.$('busca').value, '');
  t.pedidos[1].resolve([]);
  await leitura;
});

test('relatórios descartam consultas antigas e bloqueiam CSV pendente ou falho', async () => {
  const t = await pagina('gestao-relatorios.js');
  t.executar('renderCartoes = (id, dados) => { if(id === "cartoes-vendas") $("resultado").textContent = dados[0][1]; }; renderGrafico = () => {}; renderTabela = () => {}; baixarCsv = () => $("exportado").textContent = "sim";');
  const antiga = t.executar('carregar()');
  t.executar('exportar("dias")');
  assert.equal(t.$('exportado').textContent, undefined);
  assert.ok(t.csvs.every(b => b.disabled));
  const atual = t.executar('carregar()');
  t.pedidos[2].resolve(relatorio(20));
  t.pedidos[3].resolve({});
  await atual;
  assert.ok(t.csvs.every(b => !b.disabled));
  t.pedidos[0].resolve(relatorio(10));
  t.pedidos[1].resolve({});
  await antiga;
  assert.equal(t.$('resultado').textContent, '20');
  const falha = t.executar('carregar()');
  t.pedidos[4].reject(new Error('Falha atual'));
  t.pedidos[5].resolve({});
  await falha;
  t.executar('exportar("dias")');
  assert.equal(t.$('exportado').textContent, undefined);
  assert.equal(t.$('erro').textContent, 'Falha atual');
});

test('detalhe ignora resposta e erro de aberturas anteriores', async () => {
  const t = await pagina('vendas-historico.js');
  const antigo = t.executar('abrirDetalhe(1)');
  const falho = t.executar('abrirDetalhe(2)');
  const atual = t.executar('abrirDetalhe(3)');
  t.pedidos[2].resolve({ id: 3, itens: [], status: 'CONCLUIDA' });
  await atual;
  t.pedidos[0].resolve({ id: 1, itens: [], status: 'CONCLUIDA' });
  t.pedidos[1].reject(new Error('Detalhe antigo'));
  await Promise.all([antigo, falho]);
  assert.equal(t.$('detalhe-titulo').textContent, 'Venda #3');
  assert.equal(t.$('erro').hidden, true);
});

test('erro de redefinição não contamina diálogo reaberto após Escape', async () => {
  const t = await pagina('gestao-usuarios.js');
  t.executar('abrirSenha({id: 1, nome: "A", login: "a"})');
  const envio = pendente();
  t.contexto.enviar = () => envio.promise;
  const operacao = t.executar('salvarSenha({preventDefault(){}})');
  t.$('janela-senha').events.cancel();
  t.$('janela-senha').close();
  t.executar('abrirSenha({id: 1, nome: "A", login: "a"})');
  envio.reject(new Error('Falha antiga'));
  await operacao;
  assert.equal(t.$('erro-senha').hidden, true);
  assert.equal(t.$('salvar-senha').disabled, false);
  assert.equal(t.$('janela-senha').open, true);
});

test('erro de cancelamento não contamina detalhe reaberto após Escape', async () => {
  const t = await pagina('vendas-historico.js');
  const abrir = t.executar('abrirDetalhe(1)');
  t.pedidos[0].resolve({ id: 1, itens: [], status: 'CONCLUIDA' });
  await abrir;
  t.$('motivo-cancelamento').value = 'Motivo';
  const envio = pendente();
  t.contexto.enviar = () => envio.promise;
  const operacao = t.executar('cancelarVenda()');
  t.$('detalhe').events.cancel();
  t.$('detalhe').close();
  const reaberto = t.executar('abrirDetalhe(1)');
  t.pedidos[1].resolve({ id: 1, itens: [], status: 'CONCLUIDA' });
  await reaberto;
  envio.reject(new Error('Falha antiga'));
  await operacao;
  assert.equal(t.$('erro-detalhe').hidden, true);
  assert.equal(t.$('cancelar-venda').disabled, false);
  assert.equal(t.$('detalhe').open, true);
});

for (const mesmoAlvo of [false, true]) {
  test(`senha preserva diálogo reaberto (${mesmoAlvo ? 'mesmo' : 'outro'} alvo) e anuncia alvo original`, async () => {
    const t = await pagina('gestao-usuarios.js');
    t.executar('abrirSenha({id: 1, nome: "A", login: "a"})');
    const envio = pendente();
    let quantidade = 0;
    t.contexto.enviar = () => { quantidade++; return envio.promise; };
    const operacao = t.executar('salvarSenha({preventDefault(){}})');
    const duplicada = t.executar('salvarSenha({preventDefault(){}})');
    t.$('cancelar-senha').events.click();
    t.executar(`abrirSenha({id: ${mesmoAlvo ? 1 : 2}, nome: "${mesmoAlvo ? 'A' : 'B'}", login: "b"})`);
    t.$('nova-senha').value = 'nova digitacao';
    envio.resolve({});
    await Promise.all([operacao, duplicada]);
    assert.equal(quantidade, 1);
    assert.equal(t.$('janela-senha').open, true);
    assert.equal(t.$('nova-senha').value, 'nova digitacao');
    assert.match(t.$('aviso').textContent, /^Senha de A redefinida/);
  });

  test(`cancelamento preserva detalhe reaberto (${mesmoAlvo ? 'mesma' : 'outra'} venda)`, async () => {
    const t = await pagina('vendas-historico.js');
    const venda = id => ({ id, itens: [], status: 'CONCLUIDA' });
    const abrir = t.executar('abrirDetalhe(1)');
    t.pedidos[0].resolve(venda(1));
    await abrir;
    t.$('motivo-cancelamento').value = 'Motivo';
    const envio = pendente();
    let quantidade = 0;
    t.contexto.enviar = () => { quantidade++; return envio.promise; };
    const operacao = t.executar('cancelarVenda()');
    const duplicada = t.executar('cancelarVenda()');
    t.$('fechar-detalhe').events.click();
    const reaberto = t.executar(`abrirDetalhe(${mesmoAlvo ? 1 : 2})`);
    t.pedidos[1].resolve(venda(mesmoAlvo ? 1 : 2));
    await reaberto;
    envio.resolve({});
    await new Promise(setImmediate);
    for (const pedido of t.pedidos.slice(2)) pedido.resolve({ vendas: [], porForma: [] });
    await Promise.all([operacao, duplicada]);
    assert.equal(quantidade, 1);
    assert.equal(t.$('detalhe').open, true);
    assert.match(t.$('aviso').textContent, /^Venda #1 cancelada/);
  });
}
