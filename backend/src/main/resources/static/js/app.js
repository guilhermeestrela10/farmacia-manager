const DIAS_VENCIMENTO = 60;
const DIAS_GRAFICO = 14;
const NS_SVG = "http://www.w3.org/2000/svg";

/* ---------- Apoio ---------- */

function dias(n) {
  return n === 1 ? "1 dia" : n + " dias";
}

function prazo(diasParaVencer) {
  if (diasParaVencer < 0) return "venceu há " + dias(-diasParaVencer);
  if (diasParaVencer === 0) return "vence hoje";
  return "vence em " + dias(diasParaVencer);
}

function isoLocal(data) {
  return data.toLocaleDateString("sv-SE");
}

function rotuloDia(chave) {
  return chave.slice(8, 10) + "/" + chave.slice(5, 7);
}

function dataHoraCurta(iso) {
  return new Date(iso).toLocaleString("pt-BR",
    { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" });
}

/* ---------- Cartões ---------- */

function renderCartoes(r) {
  const comEstoque = r.medicamentosAtivos - r.medicamentosSemEstoque;

  const cartoes = [
    { titulo: "Medicamentos", valor: r.medicamentosAtivos, nota: "ativos",
      icone: "pilula", tom: "vermelho" },
    { titulo: "Com estoque", valor: comEstoque, nota: "medicamentos disponíveis",
      icone: "camadas", tom: "verde" },
    { titulo: "Próx. vencimento", valor: r.lotesVencendoEmBreve,
      nota: "em até " + dias(r.diasAlertaVencimento), icone: "calendario", tom: "amarelo" },
    { titulo: "Lotes vencidos", valor: r.lotesVencidosComSaldo,
      nota: r.unidadesVencidas + " un. com saldo", icone: "alerta", tom: "vermelho" },
    { titulo: "Estoque baixo", valor: r.medicamentosComEstoqueBaixo,
      nota: "e " + r.medicamentosSemEstoque + " sem estoque", icone: "queda", tom: "amarelo" },
  ];

  const area = $("cartoes");
  area.replaceChildren();

  for (const c of cartoes) {
    const selo = criar("span", "selo-icone tom-" + c.tom);
    selo.append(icone(c.icone, 20));

    const cartao = criar("div", "cartao-dash");
    cartao.append(criar("span", "titulo", c.titulo), selo,
      criar("b", "", c.valor), criar("small", "", c.nota));
    area.append(cartao);
  }
}

/* ---------- Gráfico (SVG desenhado à mão) ---------- */

function no(tag, atributos, texto) {
  const elemento = document.createElementNS(NS_SVG, tag);
  for (const [chave, valor] of Object.entries(atributos)) {
    elemento.setAttribute(chave, String(valor));
  }
  if (texto !== undefined) elemento.textContent = texto;
  return elemento;
}

function somarPorDia(movimentacoes, chaves) {
  const totais = new Map(chaves.map((chave) => [chave, 0]));
  for (const m of movimentacoes) {
    const chave = isoLocal(new Date(m.dataHora));
    if (totais.has(chave)) totais.set(chave, totais.get(chave) + m.quantidade);
  }
  return chaves.map((chave) => totais.get(chave));
}

function desenharSerie(svg, chaves, valores, cor, nome, x, y) {
  const pontos = valores.map((v, i) => x(i) + "," + y(v)).join(" ");
  svg.append(no("polyline", { points: pontos, fill: "none", stroke: cor, "stroke-width": 2 }));

  valores.forEach((v, i) => {
    const ponto = no("circle", { cx: x(i), cy: y(v), r: 4, fill: cor });
    ponto.append(no("title", {}, rotuloDia(chaves[i]) + " · " + nome + ": " + v + " un."));
    svg.append(ponto);
  });
}

function renderGrafico(chaves, entradas, saidas) {
  const largura = 720;
  const altura = 260;
  const margemEsq = 40;
  const margemDir = 12;
  const margemTopo = 12;
  const margemBase = 30;

  const maior = Math.max(4, ...entradas, ...saidas);
  const passo = Math.ceil(maior / 4);
  const limite = passo * 4;

  const x = (i) => margemEsq + (i * (largura - margemEsq - margemDir)) / (chaves.length - 1);
  const y = (v) => margemTopo + (altura - margemTopo - margemBase) * (1 - v / limite);

  const svg = no("svg", {
    viewBox: "0 0 " + largura + " " + altura,
    width: "100%",
    role: "img",
    "aria-label": "Entradas e saídas dos últimos " + DIAS_GRAFICO + " dias",
  });

  for (let t = 0; t <= 4; t++) {
    const valor = t * passo;
    svg.append(
      no("line", { x1: margemEsq, x2: largura - margemDir, y1: y(valor), y2: y(valor),
        stroke: "#e3e3e3", "stroke-dasharray": "3 3" }),
      no("text", { x: margemEsq - 6, y: y(valor) + 4, "text-anchor": "end",
        "font-size": 11, fill: "#555" }, valor));
  }

  chaves.forEach((chave, i) => {
    svg.append(no("text", { x: x(i), y: altura - 8, "text-anchor": "middle",
      "font-size": 10, fill: "#555" }, rotuloDia(chave)));
  });

  desenharSerie(svg, chaves, entradas, "#0e8a4f", "Entradas", x, y);
  desenharSerie(svg, chaves, saidas, "#d4001a", "Saídas", x, y);

  $("grafico").replaceChildren(svg);
}

/* ---------- Listas ---------- */

function renderAlertas(semEstoque, baixo, vencidos, vencendo) {
  const itens = [];

  for (const a of semEstoque) {
    itens.push({ texto: a.medicamentoNome + ": sem estoque", nivel: "critico" });
  }
  for (const l of vencidos) {
    itens.push({ texto: l.medicamentoNome + " · lote " + l.numeroLote + ": "
      + prazo(l.diasParaVencer) + " (" + l.quantidadeAtual + " un.)", nivel: "critico" });
  }
  for (const a of baixo) {
    itens.push({ texto: a.medicamentoNome + ": estoque baixo (" + a.saldoDisponivel
      + " disponíveis, mínimo " + a.estoqueMinimo + ")", nivel: "atencao" });
  }
  for (const l of vencendo) {
    itens.push({ texto: l.medicamentoNome + " · lote " + l.numeroLote + ": "
      + prazo(l.diasParaVencer) + " (" + l.quantidadeAtual + " un.)", nivel: "atencao" });
  }

  const lista = $("lista-alertas");
  lista.replaceChildren();

  if (itens.length === 0) {
    lista.append(criar("li", "nivel-vazio", "Nenhum alerta no momento."));
    return;
  }

  const limite = 8;
  for (const item of itens.slice(0, limite)) {
    lista.append(criar("li", item.nivel === "atencao" ? "nivel-atencao" : "", item.texto));
  }
  if (itens.length > limite) {
    lista.append(criar("li", "nivel-vazio", "e mais " + (itens.length - limite) + " alertas."));
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

/* ---------- Carregamento ---------- */

async function carregar() {
  limparErro();

  const inicio = new Date();
  inicio.setDate(inicio.getDate() - (DIAS_GRAFICO - 1));
  const periodo = "&de=" + isoLocal(inicio) + "&limite=500";

  try {
    const [resumo, semEstoque, baixo, vencendo, vencidos,
      entradas14, saidas14, ultimasEntradas, ultimasSaidas] = await Promise.all([
      requisitar("/alertas/resumo?dias=" + DIAS_VENCIMENTO),
      requisitar("/alertas/sem-estoque"),
      requisitar("/alertas/estoque-baixo"),
      requisitar("/alertas/vencimento-proximo?dias=" + DIAS_VENCIMENTO),
      requisitar("/alertas/vencidos"),
      requisitar("/estoque/movimentacoes?tipo=ENTRADA" + periodo),
      requisitar("/estoque/movimentacoes?tipo=SAIDA" + periodo),
      requisitar("/estoque/movimentacoes?tipo=ENTRADA&limite=5"),
      requisitar("/estoque/movimentacoes?tipo=SAIDA&limite=5"),
    ]);

    renderCartoes(resumo);

    const chaves = [];
    for (let i = DIAS_GRAFICO - 1; i >= 0; i--) {
      const dia = new Date();
      dia.setDate(dia.getDate() - i);
      chaves.push(isoLocal(dia));
    }
    renderGrafico(chaves, somarPorDia(entradas14, chaves), somarPorDia(saidas14, chaves));

    renderAlertas(semEstoque, baixo, vencidos, vencendo);

    renderLista("lista-vencimento", vencendo.slice(0, 5),
      (l) => [l.medicamentoNome + " · lote " + l.numeroLote,
        prazo(l.diasParaVencer) + " (" + dataBR(l.validade) + ") · " + l.quantidadeAtual + " un."],
      "Nenhum lote nos próximos " + DIAS_VENCIMENTO + " dias.");

    renderLista("lista-entradas", ultimasEntradas,
      (m) => [m.medicamentoNome, "+" + m.quantidade + " un. · lote " + m.numeroLote
        + " · " + dataHoraCurta(m.dataHora) + " · " + m.responsavel],
      "Sem registros.");

    renderLista("lista-saidas", ultimasSaidas,
      (m) => [m.medicamentoNome, "−" + m.quantidade + " un. · lote " + m.numeroLote
        + " · " + dataHoraCurta(m.dataHora) + " · " + m.responsavel],
      "Sem registros.");
  } catch (erro) {
    mostrarErro("Não foi possível carregar o painel. " + erro.message);
  }
}

carregar();