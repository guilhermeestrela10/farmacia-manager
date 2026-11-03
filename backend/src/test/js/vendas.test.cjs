const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

async function pagina() {
  const elementos = new Map();
  const criar = () => ({ value: '', append() {}, replaceChildren() {}, addEventListener() {}, classList: { add() {} } });
  const $ = id => { if (!elementos.has(id)) elementos.set(id, criar()); return elementos.get(id); };
  const contexto = vm.createContext({ $, criar, URLSearchParams, limparErro() {}, mostrarErro() {},
    moeda: String, setTimeout() {}, clearTimeout() {}, requisitar: async () => [] });
  vm.runInContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/vendas.js'), 'utf8'), contexto);
  await new Promise(setImmediate);
  const executar = codigo => vm.runInContext(codigo, contexto);
  executar('carrinho = [{id: 1, nome: "Teste", preco: 10, disponivel: 5, quantidade: 1}]; mostrarRecibo = () => { throw new Error("Recibo inesperado"); };');
  $('forma').value = 'DINHEIRO';
  $('recebido').value = '10';
  return { $, contexto, executar };
}

test('envia preço confirmado e preserva carrinho quando o servidor recusa preço alterado', async () => {
  const t = await pagina();
  let pedido;
  t.contexto.enviar = async (_metodo, _url, corpo) => { pedido = corpo; throw new Error('Preço alterado.'); };
  await t.executar('finalizar()');
  assert.equal(pedido.itens[0].precoEsperado, 10);
  assert.equal(t.$('erro-venda').textContent, 'Preço alterado.');
  assert.equal(t.executar('carrinho.length'), 1);
  assert.equal(t.executar('enviando'), false);
});

test('não muda ou limpa carrinho enquanto a venda está sendo enviada', async () => {
  const t = await pagina();
  let rejeitar;
  t.contexto.enviar = () => new Promise((_resolve, reject) => { rejeitar = reject; });
  const venda = t.executar('finalizar()');
  t.executar('alterarQuantidade(carrinho[0], 3); adicionar({id: 2, nome: "Outro", preco: 20, disponivel: 4}); limparCarrinho();');
  const quantidade = t.executar('carrinho[0]?.quantidade');
  const tamanho = t.executar('carrinho.length');
  rejeitar(new Error('Cancelado no teste'));
  await venda;
  assert.equal(quantidade, 1);
  assert.equal(tamanho, 1);
});
