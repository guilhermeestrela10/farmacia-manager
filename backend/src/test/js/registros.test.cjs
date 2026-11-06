const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const base = path.join(__dirname, '../../main/resources/static');
function ambiente() {
  const elementos = new Map();
  function elemento() { return {value:'', hidden:false, disabled:false, children:[], dataset:{}, style:{}, listeners:{},
    append(...items){this.children.push(...items);}, replaceChildren(...items){this.children=items;},
    addEventListener(nome, fn){this.listeners[nome]=fn;}, setAttribute(nome, valor){this[nome]=valor;},
    querySelector(){return this.fieldset ||= elemento();}, reset(){}, focus(){}, showModal(){this.open=true;}, close(){this.open=false;}}; }
  const $ = id => {if(!elementos.has(id))elementos.set(id, elemento());return elementos.get(id);};
  const contexto = vm.createContext({$, criar: (tag,classe,texto)=>Object.assign(elemento(),{tag,className:classe,textContent:texto}),
    URLSearchParams, Date, console, sair(){}, moeda:n=>'R$ '+n, dataBR:n=>n,
    requisitar:async()=>[], enviar:async()=>({}), mostrarErro:mensagem=>{$('erro').textContent=mensagem;$('erro').hidden=false;}, limparErro:()=>{$('erro').hidden=true;}});
  function executar(nome){vm.runInContext(fs.readFileSync(path.join(base,'js',nome),'utf8'),contexto);}
  executar('registros.js');
  return {$,contexto,executar,run:code=>vm.runInContext(code,contexto)};
}
function pendente(){let resolve,reject;const promise=new Promise((s,n)=>{resolve=s;reject=n;});return {promise,resolve,reject};}
test('carga descarta resposta e erro antigos, mesmo retornando ao mesmo filtro', async()=>{
  const t=ambiente(), a=pendente(),b=pendente(),c=pendente();
  t.contexto.pedidos=[a.promise,b.promise,c.promise]; t.contexto.resultados=[];
  const cargas=t.run('[0,1,2].map(i=>R.carregar("lista",()=>pedidos[i],x=>resultados.push(x)))');
  c.resolve('atual');await cargas[2]; b.reject(new Error('antigo'));await cargas[1];a.resolve('antigo');await cargas[0];
  assert.deepEqual(Array.from(t.contexto.resultados),['atual']);assert.equal(t.$('erro').hidden,true);
});
test('envio bloqueia duplo clique, preserva formulário no erro e libera nova tentativa', async()=>{
  const t=ambiente(), pedido=pendente();t.contexto.pedido=pedido.promise;t.contexto.envios=0;
  t.run('R.formulario("formulario",async()=>{envios++;await pedido;})');
  const evento={preventDefault(){}};const a=t.$('formulario').listeners.submit(evento);await t.$('formulario').listeners.submit(evento);
  assert.equal(t.contexto.envios,1);assert.equal(t.$('formulario').querySelector().disabled,true);
  pedido.reject(new Error('Falha de rede'));await a;
  assert.equal(t.$('formulario').querySelector().disabled,false);assert.equal(t.$('erro').textContent,'Falha de rede');
  t.contexto.pedido=Promise.resolve();await t.$('formulario').listeners.submit(evento);assert.equal(t.contexto.envios,2);
});
test('tabela trata conteúdo externo como texto e mostra estado vazio',()=>{
  const t=ambiente();t.run('R.tabela("corpo", [["<img src=x onerror=alert(1)>"]])');
  assert.equal(t.$('corpo').children[0].children[0].textContent,'<img src=x onerror=alert(1)>');
  t.run('R.tabela("corpo", [])');assert.equal(t.$('corpo').children[0].children[0].textContent,'Nenhum registro encontrado.');
});
for (const nome of ['clientes','lotes','atendimento','receitas','dispensacoes','gestao-configuracoes']) {
  test(`${nome}: página acessível com scripts e identificação do menu`,()=>{
    const html=fs.readFileSync(path.join(base,nome+'.html'),'utf8');
    assert.match(html,/<main[ >]/);assert.match(html,/role="alert"/);assert.match(html,/aria-live="polite"/);
    assert.match(html,new RegExp('js/'+nome+'\\.js'));assert.match(html,/js\/registros\.js/);
    assert.equal((html.match(/<main[ >]/g)||[]).length,1);assert.doesNotMatch(fs.readFileSync(path.join(base,'js',nome+'.js'),'utf8'),/innerHTML/);
  });
}
const turno = () => new Promise(setImmediate);
test('receita adiciona/remove itens, rejeita repetição e envia somente contrato previsto', async()=>{
  const t=ambiente(); const envios=[];
  t.contexto.requisitar=async url=>url==='/clientes'?[{id:1,nome:'Cliente',ativo:true}]:url==='/medicamentos'?[{id:2,nome:'<Produto>',ativo:true}]:[];
  t.contexto.enviar=async(metodo,url,corpo)=>{envios.push({metodo,url,corpo:JSON.parse(JSON.stringify(corpo))});return {};};
  t.executar('receitas.js');await turno();
  t.$('medicamento').value='2';t.$('quantidade').value='3';t.$('adicionar').listeners.click();
  t.$('adicionar').listeners.click();assert.match(t.$('erro').textContent,/já adicionado/);assert.equal(t.$('itens').children.length,1);
  t.$('quantidade').value='1.5';t.$('adicionar').listeners.click();assert.match(t.$('erro').textContent,/inteira/);
  t.$('itens').children[0].children[1].listeners.click();await turno();assert.equal(t.$('itens').children.length,0);
  t.$('quantidade').value='3';t.$('adicionar').listeners.click();
  for(const [id,value] of Object.entries({cliente:'1',prescritor:'Profissional',numero:'EX-1','emitida-em':'2026-01-01','validade-ate':'2026-12-01',observacoes:'Exemplo'}))t.$(id).value=value;
  await t.$('formulario').listeners.submit({preventDefault(){}});
  assert.deepEqual(envios,[{metodo:'POST',url:'/receitas',corpo:{clienteId:1,prescritor:'Profissional',numero:'EX-1',emitidaEm:'2026-01-01',validadeAte:'2026-12-01',observacoes:'Exemplo',itens:[{medicamentoId:2,quantidade:3}]}}]);
  assert.equal(t.$('itens').children.length,0);
});
test('receita recusa falta de itens, mais de20itens e datas incoerentes',async()=>{
  const t=ambiente();let envios=0;
  t.contexto.requisitar=async url=>url==='/clientes'?[]:url==='/medicamentos'?Array.from({length:21},(_,i)=>({id:i+1,nome:'M'+i,ativo:true})):[];
  t.contexto.enviar=async()=>{envios++;};t.executar('receitas.js');await turno();
  await t.$('formulario').listeners.submit({preventDefault(){}});assert.match(t.$('erro').textContent,/pelo menos/);
  for(let i=1;i<=21;i++){t.$('medicamento').value=String(i);t.$('quantidade').value='1';t.$('adicionar').listeners.click();}
  assert.equal(t.$('itens').children.length,20);assert.match(t.$('erro').textContent,/até 20/);
  t.$('emitida-em').value='2026-02-01';t.$('validade-ate').value='2026-01-01';
  await t.$('formulario').listeners.submit({preventDefault(){}});assert.match(t.$('erro').textContent,/datas coerentes/);assert.equal(envios,0);
});
test('dispensação usa saldo restante, preço esperado e pagamento; informa venda e recarrega saldo',async()=>{
  const t=ambiente(),envios=[];let consultas=0;
  const receita={id:3,numero:'R3',clienteNome:'Cliente',emitidaEm:'2026-01-01',validadeAte:'2026-12-31',itens:[{medicamentoId:4,medicamentoNome:'Med',quantidade:8,restante:3,preco:12.34},{medicamentoId:5,restante:0,preco:5}]};
  t.contexto.requisitar=async url=>{if(url==='/receitas')return [receita];if(url==='/receitas/3'){consultas++;return receita;}return [];};
  t.contexto.enviar=async(metodo,url,corpo)=>{envios.push({metodo,url,corpo:JSON.parse(JSON.stringify(corpo))});return {vendaId:9,total:24.68};};
  t.executar('dispensacoes.js');await turno();t.$('receita').value='3';await t.$('receita').listeners.change();
  assert.equal(t.$('itens').children.length,1);const quantidade=t.$('itens').children[0].children[2];assert.equal(quantidade.max,'3');
  quantidade.value='4';await t.$('formulario').listeners.submit({preventDefault(){}});assert.equal(envios.length,0);assert.match(t.$('erro').textContent,/saldo restante/);
  quantidade.value='2';quantidade.listeners.input();assert.equal(t.$('total').textContent,'Total: R$ 24.68');t.$('pagamento').value='PIX';
  await t.$('formulario').listeners.submit({preventDefault(){}});
  assert.deepEqual(envios,[{metodo:'POST',url:'/dispensacoes',corpo:{receitaId:3,formaPagamento:'PIX',itens:[{medicamentoId:4,quantidade:2,precoEsperado:12.34}]}}]);
  assert.match(t.$('aviso').textContent,/Venda nº 9/);assert.equal(consultas,2);assert.equal(t.$('itens').children[0].children[2].value,'0');
});
test('troca de receita remove itens antigos imediatamente e ignora resposta atrasada',async()=>{
  const t=ambiente(),a=pendente(),b=pendente();
  t.contexto.requisitar=url=>url==='/receitas/1'?a.promise:url==='/receitas/2'?b.promise:Promise.resolve([]);
  t.executar('dispensacoes.js');await turno();
  t.$('receita').value='1';const primeira=t.$('receita').listeners.change();
  t.$('receita').value='2';const segunda=t.$('receita').listeners.change();
  assert.equal(t.$('itens').children.length,0);
  const receita=id=>({id,numero:String(id),clienteNome:'Cliente',emitidaEm:'2026-01-01',validadeAte:'2026-12-31',itens:[{medicamentoId:id,medicamentoNome:'M'+id,restante:1,preco:2}]});
  b.resolve(receita(2));await segunda;a.resolve(receita(1));await primeira;
  assert.match(t.$('resumo-receita').textContent,/Receita 2/);assert.match(t.$('itens').children[0].children[0].textContent,/M2/);
});
for(const perfil of ['ADMIN','VENDEDOR'])test('configurações: perfil '+perfil,async()=>{
  const t=ambiente(),envios=[];t.$('formulario').querySelector().disabled=true;
  t.contexto.requisitar=async url=>url==='/auth/me'?{perfil}:{nomeFarmacia:'Nome anterior',telefone:'123',email:'',endereco:'Rua'};
  t.contexto.enviar=async(metodo,url,corpo)=>{envios.push({metodo,url,corpo:JSON.parse(JSON.stringify(corpo))});};
  t.contexto.atualizarNomeFarmacia=corpo=>{t.contexto.nomeLayout=corpo.nomeFarmacia;};
  t.executar('gestao-configuracoes.js');await turno();
  assert.equal(t.$('nome-farmacia').value,'Nome anterior');assert.equal(t.$('formulario').querySelector().disabled,perfil!=='ADMIN');
  if(perfil==='ADMIN'){
    t.$('nome-farmacia').value='Nova farmácia';await t.$('formulario').listeners.submit({preventDefault(){}});
    assert.deepEqual(envios,[{metodo:'PUT',url:'/gestao/configuracoes',corpo:{nomeFarmacia:'Nova farmácia',telefone:'123',email:'',endereco:'Rua'}}]);assert.equal(t.contexto.nomeLayout,'Nova farmácia');
  } else assert.match(t.$('erro').textContent,/administradores/);
});
test('lotes monta filtro escapado e link para medicamento do lote',async()=>{
  const t=ambiente();let url;
  t.$('busca').value='a&b';t.$('situacao').value='PROXIMO';t.contexto.requisitar=async caminho=>{url=caminho;return [{medicamentoId:7,medicamentoNome:'M',numeroLote:'L',validade:'2026-11-01',quantidadeAtual:2}];};
  t.executar('lotes.js');await turno();assert.equal(url,'/estoque/lotes?busca=a%26b&situacao=PROXIMO');
  assert.equal(t.$('corpo').children[0].children[4].children[0].href,'estoque.html?medicamentoId=7');
});
test('layout liga seis menus reais e aplica nome externo com textContent',()=>{
  const t=ambiente(),marca={},slogan={};
  t.contexto.document={querySelector:selector=>selector==='.marca-pequena'?marca:selector==='.slogan'?slogan:null};
  t.executar('layout.js');
  assert.equal(t.run('MENU.flatMap(s=>s.itens).filter(i=>["lotes","clientes","atendimento","receitas","dispensacoes","configuracoes"].includes(i[0])).every(i=>i[3] && i[3].endsWith(".html"))'),true);
  t.run('atualizarNomeFarmacia({nomeFarmacia:"<b>Nome</b>"})');assert.equal(marca.textContent,'<b>Nome</b>');assert.equal(slogan.textContent,'<b>Nome</b> · A saúde da comunidade');
});
test('estoque abre medicamento vindo do link de lote',async()=>{
  const t=ambiente();const urls=[];t.contexto.location={search:'?medicamentoId=7'};
  t.contexto.requisitar=async url=>{urls.push(url);if(url==='/medicamentos')return [{id:7,nome:'M'}];if(url.endsWith('/saldo'))return {saldoDisponivel:2,saldoTotal:2,saldoVencido:0,estoqueMinimo:0};return [];};
  t.executar('estoque.js');await turno();assert.equal(t.$('medicamento').value,'7');assert.ok(urls.includes('/estoque/medicamentos/7/lotes'));assert.equal(t.$('area-lotes').hidden,false);
});
test('clientes cadastra e alterna situação sem exclusão',async()=>{
  const t=ambiente(),envios=[];
  t.contexto.requisitar=async()=>[{id:5,nome:'Cliente',telefone:'',email:'',ativo:true}];
  t.contexto.enviar=async(metodo,url,corpo)=>{envios.push({metodo,url,corpo:corpo&&JSON.parse(JSON.stringify(corpo))});};
  t.executar('clientes.js');await turno();
  const botao=t.$('corpo').children[0].children[4].children[0];await botao.listeners.click();
  t.$('nome').value=' Novo ';t.$('telefone').value=' 123 ';t.$('email').value=' a@example.com ';
  await t.$('formulario').listeners.submit({preventDefault(){}});
  assert.deepEqual(envios,[{metodo:'PATCH',url:'/clientes/5/desativar',corpo:undefined},{metodo:'POST',url:'/clientes',corpo:{nome:'Novo',telefone:'123',email:'a@example.com'}}]);
});
test('atendimento usa clientes ativos para registrar e todos para consultar histórico',async()=>{
  const t=ambiente(),envios=[],urls=[];
  t.contexto.requisitar=async url=>{urls.push(url);return url==='/clientes'?[{id:1,nome:'Ativo',ativo:true},{id:2,nome:'Inativo',ativo:false}]:[];};
  t.contexto.enviar=async(metodo,url,corpo)=>{envios.push({metodo,url,corpo:JSON.parse(JSON.stringify(corpo))});};
  t.executar('atendimento.js');await turno();
  assert.equal(t.$('cliente').children.length,2);assert.equal(t.$('filtro-cliente').children.length,3);
  t.$('cliente').value='1';t.$('profissional').value=' Nome ';t.$('observacoes').value=' Registro ';
  await t.$('formulario').listeners.submit({preventDefault(){}});
  assert.deepEqual(envios,[{metodo:'POST',url:'/atendimentos',corpo:{clienteId:1,profissional:'Nome',observacoes:'Registro'}}]);
  t.$('filtro-cliente').value='2';await t.$('filtro-cliente').listeners.change();assert.ok(urls.includes('/atendimentos?clienteId=2'));
});
test('nome salvo da farmácia prevalece sobre GET inicial atrasado do layout',async()=>{
  const t=ambiente(),pedido=pendente(),marca={},slogan={};
  t.contexto.window={matchMedia:()=>({matches:false})};
  t.contexto.document={body:{dataset:{},prepend(){}},querySelector:s=>s==='main'?{}:s==='.marca-pequena'?marca:s==='.slogan'?slogan:null,querySelectorAll:()=>[]};
  t.contexto.requisitar=url=>url==='/configuracoes'?pedido.promise:Promise.resolve({nome:'Usuário',perfil:'ADMIN'});
  t.executar('layout.js');t.run('atualizarNomeFarmacia({nomeFarmacia:"Nome salvo"})');
  pedido.resolve({nomeFarmacia:'Nome antigo'});await turno();assert.equal(marca.textContent,'Nome salvo');
});
