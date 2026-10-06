let categorias = [];
let fabricantes = [];
let medicamentos = [];
let editandoId = null;
let esperaBusca;

/* ---------- Listas de apoio (categoria e fabricante) ---------- */

function preencherSelect(select, itens, selecionadoId) {
  select.replaceChildren();

  const vazio = criar("option", "", "Selecione...");
  vazio.value = "";
  select.append(vazio);

  for (const item of itens) {
    const opcao = criar("option", "", item.nome);
    opcao.value = String(item.id);
    select.append(opcao);
  }

  if (selecionadoId) select.value = String(selecionadoId);
}

async function carregarListas() {
  [categorias, fabricantes] = await Promise.all([
    requisitar("/categorias"),
    requisitar("/fabricantes"),
  ]);
  preencherSelect($("categoria"), categorias);
  preencherSelect($("fabricante"), fabricantes);
}

async function criarItem(caminho, pergunta, lista, select) {
  const nome = prompt(pergunta);
  if (!nome || !nome.trim()) return;

  try {
    const novo = await enviar("POST", caminho, { nome: nome.trim() });
    lista.push(novo);
    preencherSelect(select, lista, novo.id);
    $("erro-form").hidden = true;
  } catch (erro) {
    $("erro-form").textContent = erro.message;
    $("erro-form").hidden = false;
  }
}

/* ---------- Tabela ---------- */

function renderTabela() {
  const corpo = $("corpo");
  corpo.replaceChildren();
  $("vazio").hidden = medicamentos.length > 0;

  for (const med of medicamentos) {
    const linha = criar("tr", med.ativo ? "" : "inativo");

    const nome = criar("td");
    nome.append(criar("strong", "", med.nome));
    if (med.codigoBarras) nome.append(criar("small", "", "Cód. " + med.codigoBarras));

    const situacao = criar("td");
    situacao.append(criar("span", "selo " + (med.ativo ? "ok" : "off"),
      med.ativo ? "Ativo" : "Inativo"));

    const editar = criar("button", "btn sec", "Editar");
    editar.type = "button";
    editar.addEventListener("click", () => abrirFormulario(med));

    const alternar = criar("button", "btn sec", med.ativo ? "Desativar" : "Ativar");
    alternar.type = "button";
    alternar.addEventListener("click", () => alternarSituacao(med));

    const acoes = criar("td", "acoes");
    acoes.append(editar, alternar);

    linha.append(
      nome,
      criar("td", "", med.categoriaNome),
      criar("td", "", med.fabricanteNome),
      criar("td", "num", moeda(med.preco)),
      criar("td", "num", med.estoqueMinimo),
      situacao,
      acoes);

    corpo.append(linha);
  }
}

async function carregarLista() {
  limparErro();

  const parametros = new URLSearchParams();
  const nome = $("busca").value.trim();
  if (nome) parametros.set("nome", nome);
  if ($("inativos").checked) parametros.set("incluirInativos", "true");

  try {
    medicamentos = await requisitar("/medicamentos?" + parametros);
    renderTabela();
  } catch (erro) {
    mostrarErro(erro.message);
  }
}

function mostrarAviso(texto) {
  const aviso = $("aviso");
  aviso.textContent = texto;
  aviso.hidden = false;
  setTimeout(() => { aviso.hidden = true; }, 4000);
}

/* ---------- Formulário ---------- */

function abrirFormulario(med) {
  editandoId = med ? med.id : null;

  $("titulo-form").textContent = med ? "Editar medicamento" : "Novo medicamento";
  $("nome").value = med ? med.nome : "";
  $("codigo").value = med && med.codigoBarras ? med.codigoBarras : "";
  $("categoria").value = med ? String(med.categoriaId) : "";
  $("fabricante").value = med ? String(med.fabricanteId) : "";
  $("preco").value = med ? med.preco : "";
  $("minimo").value = med ? med.estoqueMinimo : 0;
  $("erro-form").hidden = true;

  $("janela").showModal();
}

async function salvar(evento) {
  evento.preventDefault();

  const dados = {
    nome: $("nome").value.trim(),
    codigoBarras: $("codigo").value.trim() || null,
    categoriaId: Number($("categoria").value) || null,
    fabricanteId: Number($("fabricante").value) || null,
    preco: Number($("preco").value),
    estoqueMinimo: Number($("minimo").value),
  };

  try {
    if (editandoId === null) {
      await enviar("POST", "/medicamentos", dados);
      mostrarAviso("Medicamento cadastrado.");
    } else {
      await enviar("PUT", "/medicamentos/" + editandoId, dados);
      mostrarAviso("Medicamento atualizado.");
    }
    $("janela").close();
    await carregarLista();
  } catch (erro) {
    $("erro-form").textContent = erro.message;
    $("erro-form").hidden = false;
  }
}

async function alternarSituacao(med) {
  if (med.ativo) {
    const confirmado = confirm(
      "Desativar " + med.nome + "?\n\nEle deixa de aparecer na lista e não pode " +
      "receber entradas nem saídas. O histórico é mantido.");
    if (!confirmado) return;
  }

  try {
    await enviar("PATCH", "/medicamentos/" + med.id + (med.ativo ? "/desativar" : "/ativar"));
    mostrarAviso(med.ativo ? "Medicamento desativado." : "Medicamento reativado.");
    await carregarLista();
  } catch (erro) {
    mostrarErro(erro.message);
  }
}

/* ---------- Eventos ---------- */

$("novo").addEventListener("click", () => abrirFormulario(null));
$("cancelar").addEventListener("click", () => $("janela").close());
$("formulario").addEventListener("submit", salvar);
$("inativos").addEventListener("change", carregarLista);

$("busca").addEventListener("input", () => {
  clearTimeout(esperaBusca);
  esperaBusca = setTimeout(carregarLista, 300);
});

$("nova-categoria").addEventListener("click", () =>
  criarItem("/categorias", "Nome da nova categoria:", categorias, $("categoria")));

$("novo-fabricante").addEventListener("click", () =>
  criarItem("/fabricantes", "Nome do novo fabricante:", fabricantes, $("fabricante")));

(async function iniciar() {
  try {
    await carregarListas();
  } catch (erro) {
    mostrarErro(erro.message);
  }
  await carregarLista();
})();