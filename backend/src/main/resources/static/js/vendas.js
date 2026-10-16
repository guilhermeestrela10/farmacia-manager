const LIMITE_PRODUTOS = 60;

const TIPOS = {
  REFERENCIA: ["Referência", "neutro"],
  GENERICO: ["Genérico", "bom"],
  SIMILAR: ["Similar", "atencao"],
};

const FORMAS = {
  DINHEIRO: "Dinheiro",
  PIX: "Pix",
  DEBITO: "Cartão de débito",
  CREDITO: "Cartão de crédito",
};

let produtos = [];
let carrinho = [];
let esperaBusca;
let temporizadorAviso;
let enviando = false;

/* ---------- Dinheiro em centavos (evita erro de arredondamento) ---------- */

function centavos(valor) {
  return Math.round(Number(valor) * 100);
}

function reais(valorEmCentavos) {
  return moeda(valorEmCentavos / 100);
}

function totalEmCentavos() {
  return carrinho.reduce((soma, item) => soma + centavos(item.preco) * item.quantidade, 0);
}

/* ---------- Avisos ---------- */

function mostrarAviso(texto) {
  const aviso = $("aviso");
  aviso.textContent = texto;
  aviso.hidden = false;
  clearTimeout(temporizadorAviso);
  temporizadorAviso = setTimeout(() => { aviso.hidden = true; }, 4000);
}

function mostrarErroVenda(texto) {
  $("erro-venda").textContent = texto;
  $("erro-venda").hidden = false;
}

function botao(texto, acao, classe) {
  const b = criar("button", classe || "btn sec", texto);
  b.type = "button";
  b.addEventListener("click", acao);
  return b;
}

/* ---------- Produtos ---------- */

function renderProdutos() {
  const lista = $("produtos");
  lista.replaceChildren();

  if (produtos.length === 0) {
    lista.append(criar("li", "vazio", "Nenhum medicamento encontrado."));
  }

  for (const p of produtos) {
    const [rotuloTipo, classeTipo] = TIPOS[p.tipo] || [p.tipo, "neutro"];

    const info = criar("div", "info");
    const detalhe = criar("small", "", p.categoriaNome + " · " + p.fabricanteNome + " · ");
    detalhe.append(criar("span", "selo " + classeTipo, rotuloTipo));
    info.append(criar("strong", "", p.nome), detalhe);

    const semEstoque = p.disponivel === 0;
    const preco = criar("div", "preco", moeda(p.preco));
    preco.append(criar("span", "estoque" + (semEstoque ? " zero" : ""),
      semEstoque ? "Sem estoque" : p.disponivel + " disponíveis"));

    const adicionarBotao = botao("Adicionar", () => adicionar(p), "btn");
    adicionarBotao.disabled = semEstoque;

    const linha = criar("li", "produto");
    linha.append(info, preco, adicionarBotao);
    lista.append(linha);
  }

  $("nota").textContent = produtos.length >= LIMITE_PRODUTOS
    ? "Mostrando os " + LIMITE_PRODUTOS + " primeiros. Refine a busca."
    : produtos.length + (produtos.length === 1 ? " medicamento." : " medicamentos.");
}

async function carregarProdutos() {
  limparErro();

  const parametros = new URLSearchParams();
  const busca = $("busca").value.trim();
  if (busca) parametros.set("busca", busca);
  if ($("filtro-categoria").value) parametros.set("categoriaId", $("filtro-categoria").value);
  if ($("filtro-tipo").value) parametros.set("tipo", $("filtro-tipo").value);

  try {
    produtos = await requisitar("/vendas/produtos?" + parametros);
    renderProdutos();
  } catch (erro) {
    mostrarErro(erro.message);
  }
}

async function carregarCategorias() {
  const select = $("filtro-categoria");
  const todas = criar("option", "", "Todas as categorias");
  todas.value = "";
  select.append(todas);

  try {
    for (const categoria of await requisitar("/categorias")) {
      const opcao = criar("option", "", categoria.nome);
      opcao.value = String(categoria.id);
      select.append(opcao);
    }
  } catch (erro) {
    mostrarErro(erro.message);
  }
}

/* ---------- Carrinho ---------- */

function adicionar(produto) {
  $("erro-venda").hidden = true;
  const existente = carrinho.find((item) => item.id === produto.id);

  if (existente) {
    if (existente.quantidade >= produto.disponivel) {
      mostrarErroVenda("Quantidade máxima disponível de " + produto.nome
        + ": " + produto.disponivel + ".");
      return;
    }
    existente.quantidade += 1;
  } else {
    carrinho.push({
      id: produto.id,
      nome: produto.nome,
      preco: produto.preco,
      disponivel: produto.disponivel,
      quantidade: 1,
    });
  }

  renderCarrinho();
}

function alterarQuantidade(item, nova) {
  $("erro-venda").hidden = true;

  if (!Number.isInteger(nova) || nova < 1) {
    carrinho = carrinho.filter((i) => i !== item);
  } else if (nova > item.disponivel) {
    item.quantidade = item.disponivel;
    mostrarErroVenda("Quantidade máxima disponível de " + item.nome + ": " + item.disponivel + ".");
  } else {
    item.quantidade = nova;
  }

  renderCarrinho();
}

function renderCarrinho() {
  const lista = $("itens");
  lista.replaceChildren();
  $("carrinho-vazio").hidden = carrinho.length > 0;

  for (const item of carrinho) {
    const campo = criar("input");
    campo.type = "number";
    campo.min = "1";
    campo.max = String(item.disponivel);
    campo.value = String(item.quantidade);
    campo.addEventListener("change", () => alterarQuantidade(item, Number(campo.value)));

    const remover = botao("Remover", () => alterarQuantidade(item, 0));
    remover.classList.add("remover");

    const topo = criar("div", "linha");
    topo.append(criar("strong", "", item.nome),
      criar("strong", "", reais(centavos(item.preco) * item.quantidade)));

    const controles = criar("div", "controles");
    controles.append(
      botao("−", () => alterarQuantidade(item, item.quantidade - 1)),
      campo,
      botao("+", () => alterarQuantidade(item, item.quantidade + 1)),
      criar("small", "", "× " + moeda(item.preco)),
      remover);

    const linha = criar("li", "item-carrinho");
    linha.append(topo, controles);
    lista.append(linha);
  }

  $("total").textContent = reais(totalEmCentavos());
  $("finalizar").disabled = carrinho.length === 0 || enviando;
  atualizarTroco();
}

function atualizarTroco() {
  const dinheiro = $("forma").value === "DINHEIRO";
  $("area-dinheiro").hidden = !dinheiro;

  const nota = $("troco");
  const recebido = $("recebido").value;

  if (!dinheiro || recebido === "" || carrinho.length === 0) {
    nota.textContent = "";
    return;
  }

  const diferenca = centavos(recebido) - totalEmCentavos();
  nota.textContent = diferenca >= 0
    ? "Troco: " + reais(diferenca)
    : "Faltam " + reais(-diferenca) + ".";
}

function limparCarrinho() {
  carrinho = [];
  $("recebido").value = "";
  $("erro-venda").hidden = true;
  renderCarrinho();
}

/* ---------- Finalizar ---------- */

function mostrarRecibo(venda, recebidoEmCentavos) {
  $("recibo-titulo").textContent = "Venda #" + venda.id + " registrada";
  $("recibo-info").textContent = new Date(venda.criadaEm).toLocaleString("pt-BR")
    + " · Vendedor: " + venda.vendedor;

  const lista = $("recibo-itens");
  lista.replaceChildren();
  for (const item of venda.itens) {
    const linha = criar("li");
    linha.append(
      criar("strong", "", item.quantidade + " × " + item.medicamentoNome),
      criar("span", "", moeda(item.precoUnitario) + " cada · subtotal " + moeda(item.subtotal)));
    lista.append(linha);
  }

  $("recibo-total").textContent = moeda(venda.total);

  let pagamento = "Pagamento: " + (FORMAS[venda.formaPagamento] || venda.formaPagamento) + ".";
  if (recebidoEmCentavos !== null) {
    const troco = recebidoEmCentavos - centavos(venda.total);
    pagamento += " Recebido: " + reais(recebidoEmCentavos)
      + (troco >= 0 ? ". Troco: " + reais(troco) + "." : ".");
  }
  $("recibo-pagamento").textContent = pagamento;

  $("recibo").showModal();
}

async function finalizar() {
  if (enviando || carrinho.length === 0) return;
  $("erro-venda").hidden = true;

  const forma = $("forma").value;
  const recebido = $("recebido").value;

  if (forma === "DINHEIRO" && recebido !== "" && centavos(recebido) < totalEmCentavos()) {
    mostrarErroVenda("O valor recebido é menor que o total.");
    return;
  }

  enviando = true;
  $("finalizar").disabled = true;

  try {
    const venda = await enviar("POST", "/vendas", {
      formaPagamento: forma,
      itens: carrinho.map((i) => ({ medicamentoId: i.id, quantidade: i.quantidade })),
    });

    const recebidoEmCentavos = forma === "DINHEIRO" && recebido !== "" ? centavos(recebido) : null;
    limparCarrinho();
    mostrarRecibo(venda, recebidoEmCentavos);
  } catch (erro) {
    mostrarErroVenda(erro.message);
  } finally {
    enviando = false;
    $("finalizar").disabled = carrinho.length === 0;
  }

  await carregarProdutos();
}

/* ---------- Eventos ---------- */

$("busca").addEventListener("input", () => {
  clearTimeout(esperaBusca);
  esperaBusca = setTimeout(carregarProdutos, 300);
});

// Leitor de código de barras: ele digita o código e aperta Enter.
$("busca").addEventListener("keydown", async (evento) => {
  if (evento.key !== "Enter") return;
  evento.preventDefault();
  clearTimeout(esperaBusca);
  await carregarProdutos();

  if (produtos.length === 1 && produtos[0].disponivel > 0) {
    adicionar(produtos[0]);
    $("busca").value = "";
    await carregarProdutos();
  }
});

$("filtro-categoria").addEventListener("change", carregarProdutos);
$("filtro-tipo").addEventListener("change", carregarProdutos);
$("forma").addEventListener("change", atualizarTroco);
$("recebido").addEventListener("input", atualizarTroco);
$("limpar").addEventListener("click", limparCarrinho);
$("finalizar").addEventListener("click", finalizar);
$("recibo-fechar").addEventListener("click", () => {
  $("recibo").close();
  $("busca").focus();
});

(async function iniciar() {
  renderCarrinho();
  await carregarCategorias();
  await carregarProdutos();
})();