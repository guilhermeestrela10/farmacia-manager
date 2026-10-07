let medicamentos = [];
let medicamentoAtual = null;
let saldoAtual = null;
let operacao = null;
let loteAtual = null;
let temporizadorAviso;

const TEXTOS = {
  entrada: { titulo: "Entrada de estoque", botao: "Registrar entrada" },
  saida: { titulo: "Saída de estoque", botao: "Registrar saída" },
  ajuste: { titulo: "Ajuste de lote", botao: "Registrar ajuste" },
  perda: { titulo: "Perda de lote", botao: "Registrar perda" },
};

/* ---------- Apoio ---------- */

function plural(n) {
  return n === 1 ? "1 dia" : n + " dias";
}

function diasParaVencer(validadeISO) {
  const [ano, mes, dia] = validadeISO.split("-").map(Number);
  const validade = new Date(ano, mes - 1, dia);
  const hoje = new Date();
  hoje.setHours(0, 0, 0, 0);
  return Math.round((validade - hoje) / 86400000);
}

function situacaoDoLote(dias) {
  if (dias < 0) return ["selo vencido", "Vencido há " + plural(-dias)];
  if (dias === 0) return ["selo atencao", "Vence hoje"];
  if (dias <= 90) return ["selo atencao", "Vence em " + plural(dias)];
  return ["selo bom", "Em dia"];
}

function amanhaISO() {
  const amanha = new Date();
  amanha.setDate(amanha.getDate() + 1);
  return amanha.toLocaleDateString("sv-SE");
}

function lerResponsavel() {
  try {
    return localStorage.getItem("responsavel") || "";
  } catch (erro) {
    return "";
  }
}

function guardarResponsavel(valor) {
  try {
    localStorage.setItem("responsavel", valor);
  } catch (erro) {
    // sem armazenamento: o campo só não será lembrado
  }
}

function mostrarAviso(texto) {
  const aviso = $("aviso");
  aviso.textContent = texto;
  aviso.hidden = false;
  clearTimeout(temporizadorAviso);
  temporizadorAviso = setTimeout(() => { aviso.hidden = true; }, 7000);
}

/* ---------- Escolha do medicamento ---------- */

function preencherMedicamentos(lista) {
  const select = $("medicamento");
  select.replaceChildren();

  const vazio = criar("option", "", "Selecione um medicamento...");
  vazio.value = "";
  select.append(vazio);

  for (const med of lista) {
    const opcao = criar("option", "", med.nome + " · " + med.fabricanteNome);
    opcao.value = String(med.id);
    select.append(opcao);
  }
}

async function aoEscolher() {
  const id = Number($("medicamento").value);
  medicamentoAtual = medicamentos.find((m) => m.id === id) || null;

  const escolhido = medicamentoAtual !== null;
  $("btn-entrada").disabled = !escolhido;
  $("btn-saida").disabled = !escolhido;
  $("cartoes").hidden = !escolhido;
  $("area-lotes").hidden = !escolhido;

  if (escolhido) await carregarEstoque();
}

/* ---------- Números e lotes ---------- */

function renderCartoes(s) {
  let rotuloDisponivel = "Disponível para saída";
  let classeDisponivel = "";

  if (s.saldoDisponivel === 0) {
    rotuloDisponivel += " · sem estoque";
    classeDisponivel = "alerta";
  } else if (s.estoqueBaixo) {
    rotuloDisponivel += " · estoque baixo";
    classeDisponivel = "atencao";
  }

  const cartoes = [
    [rotuloDisponivel, s.saldoDisponivel, classeDisponivel],
    ["Total em estoque", s.saldoTotal, ""],
    ["Vencido", s.saldoVencido, s.saldoVencido > 0 ? "alerta" : ""],
    ["Estoque mínimo", s.estoqueMinimo, ""],
  ];

  const area = $("cartoes");
  area.replaceChildren();
  for (const [titulo, valor, classe] of cartoes) {
    const cartao = criar("div", "cartao " + classe);
    cartao.append(criar("b", "", valor), criar("span", "", titulo));
    area.append(cartao);
  }
}

function renderLotes(lotes) {
  const corpo = $("corpo");
  corpo.replaceChildren();
  $("vazio").hidden = lotes.length > 0;

  for (const lote of lotes) {
    const dias = diasParaVencer(lote.validade);
    const [classe, texto] = situacaoDoLote(dias);

    const situacao = criar("td");
    situacao.append(criar("span", classe, texto));

    const ajuste = criar("button", "btn sec", "Ajuste");
    ajuste.type = "button";
    ajuste.addEventListener("click", () => abrirOperacao("ajuste", lote));

    const perda = criar("button", dias < 0 ? "btn" : "btn sec", "Perda");
    perda.type = "button";
    perda.addEventListener("click", () => abrirOperacao("perda", lote));

    const acoes = criar("td", "acoes");
    acoes.append(ajuste, perda);

    const linha = criar("tr");
    linha.append(
      criar("td", "", lote.numeroLote),
      criar("td", "", dataBR(lote.validade)),
      situacao,
      criar("td", "num", lote.quantidadeAtual),
      acoes);

    corpo.append(linha);
  }
}

async function carregarEstoque() {
  limparErro();

  try {
    const [saldo, lotes] = await Promise.all([
      requisitar("/estoque/medicamentos/" + medicamentoAtual.id + "/saldo"),
      requisitar("/estoque/medicamentos/" + medicamentoAtual.id + "/lotes"),
    ]);

    saldoAtual = saldo;
    renderCartoes(saldo);
    renderLotes(lotes);
  } catch (erro) {
    mostrarErro(erro.message);
  }
}

/* ---------- Janela de operação ---------- */

function atualizarLimite() {
  const campo = $("quantidade");
  let maximo = null;

  if (operacao === "saida") maximo = saldoAtual.saldoDisponivel;
  if (operacao === "perda") maximo = loteAtual.quantidadeAtual;
  if (operacao === "ajuste" && $("tipo").value === "AJUSTE_SAIDA") {
    maximo = loteAtual.quantidadeAtual;
  }

  if (maximo !== null && maximo > 0) {
    campo.max = String(maximo);
  } else {
    campo.removeAttribute("max");
  }
}

function abrirOperacao(tipo, lote) {
  operacao = tipo;
  loteAtual = lote || null;

  const entrada = tipo === "entrada";
  const ajuste = tipo === "ajuste";
  const exigeMotivo = ajuste || tipo === "perda";

  $("titulo-form").textContent = TEXTOS[tipo].titulo + " · " + medicamentoAtual.nome;
  $("confirmar").textContent = TEXTOS[tipo].botao;

  $("campo-lote").hidden = !entrada;
  $("campo-validade").hidden = !entrada;
  $("campo-tipo").hidden = !ajuste;

  $("numero-lote").required = entrada;
  $("validade").required = entrada;
  $("motivo").required = exigeMotivo;
  $("rotulo-motivo").textContent = exigeMotivo ? "Motivo (obrigatório)" : "Motivo (opcional)";

  const resumo = $("resumo-lote");
  if (lote) {
    resumo.textContent = "Lote " + lote.numeroLote + " · validade " + dataBR(lote.validade)
      + " · saldo atual " + lote.quantidadeAtual + " un.";
    resumo.hidden = false;
  } else if (tipo === "saida") {
    resumo.textContent = "Disponível para saída: " + saldoAtual.saldoDisponivel
      + " un. Sai primeiro o lote que vence antes.";
    resumo.hidden = false;
  } else {
    resumo.hidden = true;
  }

  $("numero-lote").value = "";
  $("validade").value = "";
  $("validade").min = amanhaISO();
  $("tipo").value = "AJUSTE_ENTRADA";
  $("quantidade").value = "";
  $("motivo").value = "";
  $("responsavel").value = lerResponsavel();
  $("erro-form").hidden = true;

  atualizarLimite();
  $("janela").showModal();
}

function mensagemDeSucesso(resposta) {
  if (operacao === "entrada") {
    return "Entrada registrada: " + resposta.quantidade + " un. no lote "
      + resposta.numeroLote + ".";
  }
  if (operacao === "saida") {
    return "Saída registrada: "
      + resposta.map((m) => m.quantidade + " un. do lote " + m.numeroLote).join(", ") + ".";
  }
  if (operacao === "ajuste") return "Ajuste registrado no lote " + resposta.numeroLote + ".";
  return "Perda registrada no lote " + resposta.numeroLote + ".";
}

async function salvar(evento) {
  evento.preventDefault();

  const quantidade = Number($("quantidade").value);
  const responsavel = $("responsavel").value.trim();
  const motivo = $("motivo").value.trim();

  let caminho;
  let corpo;

  if (operacao === "entrada") {
    caminho = "/estoque/entradas";
    corpo = {
      medicamentoId: medicamentoAtual.id,
      numeroLote: $("numero-lote").value.trim(),
      validade: $("validade").value,
      quantidade,
      motivo: motivo || null,
      responsavel,
    };
  } else if (operacao === "saida") {
    caminho = "/estoque/saidas";
    corpo = {
      medicamentoId: medicamentoAtual.id,
      quantidade,
      motivo: motivo || null,
      responsavel,
    };
  } else if (operacao === "ajuste") {
    caminho = "/estoque/ajustes";
    corpo = {
      loteId: loteAtual.id,
      tipo: $("tipo").value,
      quantidade,
      motivo,
      responsavel,
    };
  } else {
    caminho = "/estoque/perdas";
    corpo = {
      loteId: loteAtual.id,
      quantidade,
      motivo,
      responsavel,
    };
  }

  const botao = $("confirmar");
  botao.disabled = true;

  try {
    const resposta = await enviar("POST", caminho, corpo);
    guardarResponsavel(responsavel);
    $("janela").close();
    mostrarAviso(mensagemDeSucesso(resposta));
    await carregarEstoque();
  } catch (erro) {
    $("erro-form").textContent = erro.message;
    $("erro-form").hidden = false;
  } finally {
    botao.disabled = false;
  }
}

/* ---------- Eventos ---------- */

$("medicamento").addEventListener("change", aoEscolher);
$("btn-entrada").addEventListener("click", () => abrirOperacao("entrada"));
$("btn-saida").addEventListener("click", () => abrirOperacao("saida"));
$("cancelar").addEventListener("click", () => $("janela").close());
$("tipo").addEventListener("change", atualizarLimite);
$("formulario").addEventListener("submit", salvar);

(async function iniciar() {
  try {
    medicamentos = await requisitar("/medicamentos");
    preencherMedicamentos(medicamentos);
  } catch (erro) {
    mostrarErro(erro.message);
  }
})();