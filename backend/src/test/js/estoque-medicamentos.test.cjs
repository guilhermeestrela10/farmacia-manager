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

async function carregar(arquivo) {
  const elementos = new Map();
  const elemento = () => ({ value: '', hidden: false, disabled: false, children: [],
    append(...itens) { this.children.push(...itens); },
    replaceChildren(...itens) { this.children = itens; },
    addEventListener() {}, showModal() {}, close() {},
    removeAttribute(nome) { delete this[nome]; },
    querySelector() { return this.submit ||= elemento(); },
  });
  const $ = (id) => {
    if (!elementos.has(id)) elementos.set(id, elemento());
    return elementos.get(id);
  };
  const pedidos = [];
  const contexto = vm.createContext({ $, criar: elemento, URLSearchParams, limparErro() {},
    mostrarErro(mensagem) { $('erro').textContent = mensagem; },
    requisitar(url) {
      if (url.startsWith('/medicamentos') || url === '/categorias' || url === '/fabricantes') {
        return Promise.resolve([]);
      }
      const pedido = pendente();
      pedidos.push({ url, ...pedido });
      return pedido.promise;
    },
    enviar() { throw new Error('Envio inesperado'); },
    setTimeout() {}, clearTimeout() {},
  });
  vm.runInContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/js', arquivo), 'utf8'), contexto);
  await new Promise(setImmediate);
  return { $, pedidos, contexto, executar: (codigo) => vm.runInContext(codigo, contexto) };
}

test('estoque limpa seleção antiga e ignora respostas fora de ordem A-B-A', async () => {
  const t = await carregar('estoque.js');
  t.executar('medicamentos = [{id: 1, nome: "A"}, {id: 2, nome: "B"}]; renderCartoes = s => $("cartoes").textContent = s.saldoDisponivel; renderLotes = lotes => $("corpo").replaceChildren(...lotes);');
  t.$('medicamento').value = '1';
  const primeira = t.executar('aoEscolher()');
  t.pedidos[0].resolve({ saldoDisponivel: 10 });
  t.pedidos[1].resolve(['lote A']);
  await primeira;
  t.$('medicamento').value = '2';
  const segunda = t.executar('aoEscolher()');
  assert.equal(t.$('btn-saida').disabled, true);
  assert.equal(t.$('area-lotes').hidden, true);
  assert.equal(t.$('corpo').children.length, 0);
  t.$('medicamento').value = '1';
  const terceira = t.executar('aoEscolher()');
  t.pedidos[4].resolve({ saldoDisponivel: 30 });
  t.pedidos[5].resolve(['A novo']);
  await terceira;
  t.pedidos[2].resolve({ saldoDisponivel: 20 });
  t.pedidos[3].resolve(['B antigo']);
  await segunda;
  assert.equal(t.$('cartoes').textContent, 30);
  assert.deepEqual(t.$('corpo').children, ['A novo']);
  assert.equal(t.$('btn-saida').disabled, false);
});

test('estoque ignora resposta de A antigo após retornar para A', async () => {
  const t = await carregar('estoque.js');
  t.executar('medicamentos = [{id: 1}, {id: 2}]; renderCartoes = s => $("cartoes").textContent = s.saldoDisponivel; renderLotes = () => {};');
  const cargas = [];
  for (const id of ['1', '2', '1']) {
    t.$('medicamento').value = id;
    cargas.push(t.executar('aoEscolher()'));
  }
  for (const indice of [4, 2, 0]) {
    t.pedidos[indice].resolve({ saldoDisponivel: indice });
    t.pedidos[indice + 1].resolve([]);
    await cargas[indice / 2];
  }
  assert.equal(t.$('cartoes').textContent, 4);
});

for (const arquivo of ['estoque.js', 'medicamentos.js']) {
  test(`${arquivo} impede duplo envio e permite tentar novamente após erro`, async () => {
    const t = await carregar(arquivo);
    if (arquivo === 'estoque.js') t.executar('medicamentoAtual = {id: 1}; operacao = "entrada";');
    t.$('nome').value = 'Medicamento';
    t.$('quantidade').value = '1';
    let quantidade = 0;
    const resposta = pendente();
    t.contexto.enviar = () => { quantidade++; return resposta.promise; };
    const primeiro = t.executar('salvar({preventDefault(){}})');
    const duplicado = t.executar('salvar({preventDefault(){}})');
    const enviosIniciais = quantidade;
    resposta.reject(new Error('Falha de teste'));
    await Promise.all([primeiro, duplicado]);
    assert.equal(enviosIniciais, 1);
    assert.equal(t.$('erro-form').textContent, 'Falha de teste');
    const nova = pendente();
    t.contexto.enviar = () => { quantidade++; return nova.promise; };
    const tentativa = t.executar('salvar({preventDefault(){}})');
    assert.equal(quantidade, 2);
    nova.reject(new Error('Outra falha'));
    await tentativa;
  });
}
