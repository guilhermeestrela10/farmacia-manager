const NS_SVG = "http://www.w3.org/2000/svg";

const FORMAS = {
  DINHEIRO: "Dinheiro",
  PIX: "Pix",
  DEBITO: "Cartão de débito",
  CREDITO: "Cartão de crédito",
};

let vendas = null;
let estoque = null;
let versaoCarga = 0;

/* ---------- Apoio ---------- */

function isoLocal(data) {
  return data.toLocaleDateString("sv-SE");
}

function somarDias(data, n) {
  const nova = new Date(data);
  nova.setDate(nova.getDate() + n);
  return nova;
}

function numero(valor) {
  return Number(valor).toLocaleString("pt-BR");
}

function decimalBR(valor) {
  return Number(valor).toFixed(2).replace(".", ",");
}

function aplicarAtalho(tipo) {
  const hoje = new Date();
  let inicio = hoje;

  if (tipo === "7") inicio = somarDias(hoje, -6);
  if (tipo === "30") inicio = somarDias(hoje, -29);
  if (tipo === "mes") inicio = new Date(hoje.getFullYear(), hoje.getMonth(), 1);

  $("f-de").value = isoLocal(inicio);
  $("f-ate").value = isoLocal(hoje);
  carregar();
}

/* ---------- Cartões e tabelas ---------- */

function renderCartoes(id, cartoes) {
  const area = $(id);
  area.replaceChildren();

  for (const [titulo, valor, nota, classe] of cartoes) {
    const cartao = criar("div", "cartao-dash " + (classe || ""));
    cartao.append(criar("span", "titulo", titulo), criar("b", "", valor), criar("small", "", nota));
    area.append(cartao);
  }
}

function renderTabela(id, linhas, colunasNumericas) {
  const corpo = $(id);
  corpo.replaceChildren();

  if (linhas.length === 0) {
    const linha = criar("tr");
    const celula = criar("td", "vazio", "Sem dados no período.");
    celula.colSpan = 3;
    linha.append(celula);
    corpo.append(linha);
    return;
  }

  for (const valores of linhas) {
    const linha = criar("tr");
    valores.forEach((valor, i) => {
      linha.append(criar("td", colunasNumericas.includes(i) ? "num" : "", valor));
    });
    corpo.append(linha);
  }
}

/* ---------- Gráfico de barras (SVG desenhado à mão) ---------- */

function no(tag, atributos, texto) {
  const elemento = document.createElementNS(NS_SVG, tag);
  for (const [chave, valor] of Object.entries(atributos)) {
    elemento.setAttribute(chave, String(valor));
  }
  if (texto !== undefined) elemento.textContent = texto;
  return elemento;
}

function escalaBonita(maximo) {
  if (maximo <= 0) return 4;
  const potencia = Math.pow(10, Math.floor(Math.log10(maximo)));
  const fator = maximo / potencia;
  const bonito = fator <= 1 ? 1 : fator <= 2 ? 2 : fator <= 5 ? 5 : 10;
  return bonito * potencia;
}

function renderGrafico(porDia) {
  const largura = 760;
  const altura = 260;
  const margemEsq = 64;
  const margemDir = 12;
  const margemTopo = 12;
  const margemBase = 32;

  const maior = Math.max(...porDia.map((d) => Number(d.total)), 0);
  const teto = escalaBonita(maior);
  const areaLargura = largura - margemEsq - margemDir;
  const passoColuna = areaLargura / porDia.length;
  const larguraBarra = passoColuna * 0.7;
  const y = (valor) => margemTopo + (altura - margemTopo - margemBase) * (1 - valor / teto);

  const svg = no("svg", {
    viewBox: "0 0 " + largura + " " + altura,
    width: "100%",
    role: "img",
    "aria-label": "Total vendido por dia",
  });

  for (let t = 0; t <= 4; t++) {
    const valor = (teto / 4) * t;
    svg.append(
      no("line", { x1: margemEsq, x2: largura - margemDir, y1: y(valor), y2: y(valor),
        stroke: "#e3e3e3", "stroke-dasharray": "3 3" }),
      no("text", { x: margemEsq - 6, y: y(valor) + 4, "text-anchor": "end",
        "font-size": 11, fill: "#555" }, numero(Math.round(valor))));
  }

  const salto = Math.ceil(porDia.length / 12);

  porDia.forEach((dia, i) => {
    const x = margemEsq + i * passoColuna + (passoColuna - larguraBarra) / 2;
    const topo = y(Number(dia.total));

    const barra = no("rect", {
      x, y: topo, width: larguraBarra, height: Math.max(altura - margemBase - topo, 0),
      fill: "#c40000", rx: 2,
    });
    barra.append(no("title", {},
      dataBR(dia.data) + " · " + moeda(dia.total) + " · " + dia.quantidade + " venda(s)"));
    svg.append(barra);

    if (i % salto === 0) {
      svg.append(no("text", { x: x + larguraBarra / 2, y: altura - 10, "text-anchor": "middle",
        "font-size": 10, fill: "#555" }, dia.data.slice(8, 10) + "/" + dia.data.slice(5, 7)));
    }
  });

  $("grafico-dias").replaceChildren(svg);
}

/* ---------- Exportar CSV ---------- */

function celulaCsv(valor) {
  let texto = String(valor === null || valor === undefined ? "" : valor);
  // Evita que o Excel interprete o texto como fórmula.
  if (/^[=+\-@\t\r]/.test(texto)) texto = "'" + texto;
  return '"' + texto.replace(/"/g, '""') + '"';
}

function baixarCsv(arquivo, cabecalho, linhas) {
  const texto = [cabecalho, ...linhas].map((l) => l.map(celulaCsv).join(";")).join("\r\n");
  const blob = new Blob(["\uFEFF" + texto], { type: "text/csv;charset=utf-8" });
  const endereco = URL.createObjectURL(blob);

  const link = criar("a");
  link.href = endereco;
  link.download = arquivo;
  document.body.append(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(endereco);
}

const CSVS = {
  dias: (v) => ["vendas-por-dia", ["Data", "Vendas", "Total (R$)"],
    v.porDia.map((d) => [dataBR(d.data), d.quantidade, decimalBR(d.total)])],
  forma: (v) => ["vendas-por-pagamento", ["Forma de pagamento", "Vendas", "Total (R$)"],
    v.porForma.map((f) => [FORMAS[f.forma] || f.forma, f.quantidade, decimalBR(f.total)])],
  vendedor: (v) => ["vendas-por-vendedor", ["Vendedor", "Vendas", "Total (R$)"],
    v.porVendedor.map((x) => [x.vendedor, x.quantidade, decimalBR(x.total)])],
  categoria: (v) => ["vendas-por-categoria", ["Categoria", "Unidades", "Faturamento (R$)"],
    v.porCategoria.map((c) => [c.categoria, c.unidades, decimalBR(c.total)])],
  produtos: (v) => ["mais-vendidos", ["Medicamento", "Unidades", "Faturamento (R$)"],
    v.maisVendidos.map((p) => [p.medicamento, p.unidades, decimalBR(p.total)])],
};

function exportar(tipo) {
  if (!vendas) return;
  const [nome, cabecalho, linhas] = CSVS[tipo](vendas);
  baixarCsv(nome + "_" + vendas.de + "_a_" + vendas.ate + ".csv", cabecalho, linhas);
}

/* ---------- Carregamento ---------- */

async function carregar() {
  const versao = ++versaoCarga;
  vendas = null;
  for (const botao of document.querySelectorAll("[data-csv]")) botao.disabled = true;
  limparErro();

  const parametros = new URLSearchParams();
  if ($("f-de").value) parametros.set("de", $("f-de").value);
  if ($("f-ate").value) parametros.set("ate", $("f-ate").value);

  try {
    const respostas = await Promise.all([
      requisitar("/gestao/relatorios/vendas?" + parametros),
      requisitar("/gestao/relatorios/estoque?" + parametros),
    ]);
    if (versao !== versaoCarga) return;
    [vendas, estoque] = respostas;
  } catch (erro) {
    if (versao === versaoCarga) mostrarErro(erro.message);
    return;
  }

  renderCartoes("cartoes-vendas", [
    ["Total vendido", moeda(vendas.totalVendido), "vendas concluídas"],
    ["Vendas", numero(vendas.quantidadeVendas), "no período"],
    ["Ticket médio", moeda(vendas.ticketMedio), "por venda"],
    ["Unidades vendidas", numero(vendas.unidadesVendidas), "itens"],
    ["Canceladas", numero(vendas.quantidadeCanceladas), moeda(vendas.valorCancelado), "alerta"],
  ]);

  renderGrafico(vendas.porDia);

  renderTabela("t-forma",
    vendas.porForma.map((f) => [FORMAS[f.forma] || f.forma, numero(f.quantidade), moeda(f.total)]),
    [1, 2]);
  renderTabela("t-vendedor",
    vendas.porVendedor.map((v) => [v.vendedor, numero(v.quantidade), moeda(v.total)]),
    [1, 2]);
  renderTabela("t-categoria",
    vendas.porCategoria.map((c) => [c.categoria, numero(c.unidades), moeda(c.total)]),
    [1, 2]);
  renderTabela("t-produtos",
    vendas.maisVendidos.map((p) => [p.medicamento, numero(p.unidades), moeda(p.total)]),
    [1, 2]);

  renderCartoes("cartoes-estoque", [
    ["Estoque disponível", moeda(estoque.valorDisponivel), numero(estoque.unidadesDisponiveis) + " un."],
    ["Vence em " + estoque.diasVencimento + " dias", moeda(estoque.valorVencendo),
      numero(estoque.unidadesVencendo) + " un.", "atencao"],
    ["Vencido com saldo", moeda(estoque.valorVencido),
      numero(estoque.unidadesVencidas) + " un.", "alerta"],
    ["Perdas no período", moeda(estoque.valorPerdas),
      numero(estoque.unidadesPerdidas) + " un.", "alerta"],
  ]);
  for (const botao of document.querySelectorAll("[data-csv]")) botao.disabled = false;
}

/* ---------- Eventos ---------- */

for (const botao of document.querySelectorAll("[data-atalho]")) {
  botao.addEventListener("click", () => aplicarAtalho(botao.dataset.atalho));
}
for (const botao of document.querySelectorAll("[data-csv]")) {
  botao.addEventListener("click", () => exportar(botao.dataset.csv));
}
$("f-de").addEventListener("change", carregar);
$("f-ate").addEventListener("change", carregar);

aplicarAtalho("30");
