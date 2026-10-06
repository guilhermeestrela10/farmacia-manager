const $ = (id) => document.getElementById(id);

async function buscar(caminho) {
  const resposta = await fetch(caminho);
  if (!resposta.ok) {
    throw new Error(caminho + " respondeu " + resposta.status);
  }
  return resposta.json();
}

function criar(tag, classe, texto) {
  const elemento = document.createElement(tag);
  if (classe) elemento.className = classe;
  if (texto !== undefined) elemento.textContent = texto;
  return elemento;
}

function dataBR(iso) {
  const [ano, mes, dia] = iso.split("-");
  return dia + "/" + mes + "/" + ano;
}

function dias(n) {
  return n === 1 ? "1 dia" : n + " dias";
}

function prazo(diasParaVencer) {
  if (diasParaVencer < 0) return "venceu há " + dias(-diasParaVencer);
  if (diasParaVencer === 0) return "vence hoje";
  return "vence em " + dias(diasParaVencer);
}

function renderCartoes(r) {
  const cartoes = [
    ["Medicamentos ativos", r.medicamentosAtivos, ""],
    ["Sem estoque", r.medicamentosSemEstoque, "alerta"],
    ["Estoque baixo", r.medicamentosComEstoqueBaixo, "atencao"],
    ["Vencendo em " + dias(r.diasAlertaVencimento), r.lotesVencendoEmBreve, "atencao"],
    ["Lotes vencidos com saldo (" + r.unidadesVencidas + " un.)", r.lotesVencidosComSaldo, "alerta"],
  ];

  const area = $("cartoes");
  area.replaceChildren();
  for (const [titulo, valor, classe] of cartoes) {
    const cartao = criar("div", "cartao " + classe);
    cartao.append(criar("b", "", valor), criar("span", "", titulo));
    area.append(cartao);
  }
}

function renderLista(id, itens, montar, mensagemVazia) {
  const lista = $(id);
  lista.replaceChildren();

  if (itens.length === 0) {
    lista.append(criar("li", "vazio", mensagemVazia));
    return;
  }

  for (const item of itens) {
    const [titulo, detalhe] = montar(item);
    const linha = criar("li");
    linha.append(criar("strong", "", titulo), criar("span", "", detalhe));
    lista.append(linha);
  }
}

async function carregar() {
  const prazoEscolhido = $("dias").value;
  $("erro").hidden = true;

  try {
    const [resumo, semEstoque, baixo, vencendo, vencidos] = await Promise.all([
      buscar("/alertas/resumo?dias=" + prazoEscolhido),
      buscar("/alertas/sem-estoque"),
      buscar("/alertas/estoque-baixo"),
      buscar("/alertas/vencimento-proximo?dias=" + prazoEscolhido),
      buscar("/alertas/vencidos"),
    ]);

    renderCartoes(resumo);

    renderLista("lista-sem-estoque", semEstoque,
      (a) => [a.medicamentoNome, "Estoque mínimo: " + a.estoqueMinimo],
      "Nenhum medicamento sem estoque.");

    renderLista("lista-estoque-baixo", baixo,
      (a) => [a.medicamentoNome,
        a.saldoDisponivel + " disponíveis (mínimo " + a.estoqueMinimo + ")"],
      "Nenhum medicamento com estoque baixo.");

    renderLista("lista-vencimento", vencendo,
      (l) => [l.medicamentoNome + " · lote " + l.numeroLote,
        prazo(l.diasParaVencer) + " (" + dataBR(l.validade) + ") · " + l.quantidadeAtual + " un."],
      "Nenhum lote vencendo neste prazo.");

    renderLista("lista-vencidos", vencidos,
      (l) => [l.medicamentoNome + " · lote " + l.numeroLote,
        prazo(l.diasParaVencer) + " · " + l.quantidadeAtual + " un."],
      "Nenhum lote vencido com saldo.");
  } catch (erro) {
    $("erro").textContent =
      "Não foi possível carregar o painel. A aplicação está rodando? (" + erro.message + ")";
    $("erro").hidden = false;
  }
}

$("dias").addEventListener("change", carregar);
carregar();