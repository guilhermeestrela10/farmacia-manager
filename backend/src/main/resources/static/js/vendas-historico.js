const FORMAS = {
  DINHEIRO: "Dinheiro",
  PIX: "Pix",
  DEBITO: "Cartão de débito",
  CREDITO: "Cartão de crédito",
};

let ehAdmin = false;
let vendaAberta = null;
let temporizadorAviso;

function hojeISO() {
  return new Date().toLocaleDateString("sv-SE");
}

function dataHora(iso) {
  return new Date(iso).toLocaleString("pt-BR",
    { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" });
}

function mostrarAviso(texto) {
  const aviso = $("aviso");
  aviso.textContent = texto;
  aviso.hidden = false;
  clearTimeout(temporizadorAviso);
  temporizadorAviso = setTimeout(() => { aviso.hidden = true; }, 6000);
}

/* ---------- Totais e lista ---------- */

function renderCartoes(h) {
  const cartoes = [
    ["Total vendido", moeda(h.totalVendido), "vendas concluídas no período"],
    ["Vendas concluídas", h.quantidadeConcluidas, ""],
    ["Canceladas", h.quantidadeCanceladas, ""],
  ];

  for (const f of h.porForma) {
    cartoes.push([FORMAS[f.forma] || f.forma, moeda(f.total),
      f.quantidade + (f.quantidade === 1 ? " venda" : " vendas")]);
  }

  const area = $("cartoes");
  area.replaceChildren();

  for (const [titulo, valor, nota] of cartoes) {
    const cartao = criar("div", "cartao-dash");
    cartao.append(criar("span", "titulo", titulo), criar("b", "", valor), criar("small", "", nota));
    area.append(cartao);
  }
}

function renderTabela(h) {
  const corpo = $("corpo");
  corpo.replaceChildren();
  $("vazio").hidden = h.vendas.length > 0;

  for (const v of h.vendas) {
    const cancelada = v.status === "CANCELADA";

    const situacao = criar("td");
    situacao.append(criar("span", "selo " + (cancelada ? "vencido" : "bom"),
      cancelada ? "Cancelada" : "Concluída"));

    const ver = criar("button", "btn sec", "Ver");
    ver.type = "button";
    ver.addEventListener("click", () => abrirDetalhe(v.id));

    const acoes = criar("td", "acoes");
    acoes.append(ver);

    const linha = criar("tr", cancelada ? "inativo" : "");
    linha.append(
      criar("td", "", "#" + v.id),
      criar("td", "", dataHora(v.criadaEm)),
      criar("td", "", v.vendedor),
      criar("td", "", FORMAS[v.formaPagamento] || v.formaPagamento),
      criar("td", "num", moeda(v.total)),
      situacao,
      acoes);

    corpo.append(linha);
  }

  $("nota").textContent = h.truncado
    ? "Mostrando as 500 vendas mais recentes do período. Os totais acima consideram todas."
    : h.vendas.length + (h.vendas.length === 1 ? " venda." : " vendas.");
}

async function carregar() {
  limparErro();

  const parametros = new URLSearchParams();
  if ($("f-de").value) parametros.set("de", $("f-de").value);
  if ($("f-ate").value) parametros.set("ate", $("f-ate").value);
  if ($("f-status").value) parametros.set("status", $("f-status").value);
  if (ehAdmin && $("f-vendedor").value) parametros.set("vendedorId", $("f-vendedor").value);

  try {
    const historico = await requisitar("/vendas?" + parametros);
    renderCartoes(historico);
    renderTabela(historico);
  } catch (erro) {
    mostrarErro(erro.message);
  }
}

/* ---------- Detalhe e cancelamento ---------- */

async function abrirDetalhe(id) {
  limparErro();

  try {
    const venda = await requisitar("/vendas/" + id);
    vendaAberta = venda;

    const cancelada = venda.status === "CANCELADA";

    $("detalhe-titulo").textContent = "Venda #" + venda.id + (cancelada ? " (cancelada)" : "");
    $("detalhe-info").textContent = dataHora(venda.criadaEm) + " · Vendedor: " + venda.vendedor
      + " · " + (FORMAS[venda.formaPagamento] || venda.formaPagamento);

    const lista = $("detalhe-itens");
    lista.replaceChildren();
    for (const item of venda.itens) {
      const linha = criar("li");
      linha.append(
        criar("strong", "", item.quantidade + " × " + item.medicamentoNome),
        criar("span", "", moeda(item.precoUnitario) + " cada · subtotal " + moeda(item.subtotal)));
      lista.append(linha);
    }

    $("detalhe-total").textContent = moeda(venda.total);

    const aviso = $("detalhe-cancelamento");
    aviso.hidden = !cancelada;
    if (cancelada) {
      aviso.textContent = "Cancelada em " + dataHora(venda.canceladaEm)
        + " por " + venda.canceladaPor + ". Motivo: " + venda.motivoCancelamento;
    }

    $("area-cancelar").hidden = !(ehAdmin && !cancelada);
    $("motivo-cancelamento").value = "";
    $("erro-detalhe").hidden = true;

    $("detalhe").showModal();
  } catch (erro) {
    mostrarErro(erro.message);
  }
}

async function cancelarVenda() {
  const motivo = $("motivo-cancelamento").value.trim();
  const erro = $("erro-detalhe");

  if (!motivo) {
    erro.textContent = "Informe o motivo do cancelamento.";
    erro.hidden = false;
    return;
  }

  const confirmado = confirm(
    "Cancelar a venda #" + vendaAberta.id + "?\n\nAs unidades voltam ao estoque. "
    + "Essa ação não pode ser desfeita.");
  if (!confirmado) return;

  const botao = $("cancelar-venda");
  botao.disabled = true;

  try {
    await enviar("POST", "/vendas/" + vendaAberta.id + "/cancelar", { motivo });
    $("detalhe").close();
    mostrarAviso("Venda #" + vendaAberta.id + " cancelada. As unidades voltaram ao estoque.");
    await carregar();
  } catch (e) {
    erro.textContent = e.message;
    erro.hidden = false;
  } finally {
    botao.disabled = false;
  }
}

/* ---------- Início ---------- */

async function carregarVendedores() {
  const select = $("f-vendedor");
  const todos = criar("option", "", "Todos os vendedores");
  todos.value = "";
  select.append(todos);

  for (const usuario of await requisitar("/usuarios")) {
    const opcao = criar("option", "", usuario.nome + (usuario.ativo ? "" : " (inativo)"));
    opcao.value = String(usuario.id);
    select.append(opcao);
  }
}

(async function iniciar() {
  try {
    const eu = await requisitar("/auth/me");
    ehAdmin = eu.perfil === "ADMIN";

    if (ehAdmin) {
      $("campo-vendedor").hidden = false;
      await carregarVendedores();
    }
  } catch (erro) {
    mostrarErro(erro.message);
    return;
  }

  $("f-de").value = hojeISO();
  $("f-ate").value = hojeISO();

  for (const id of ["f-de", "f-ate", "f-status", "f-vendedor"]) {
    $(id).addEventListener("change", carregar);
  }

  $("hoje").addEventListener("click", () => {
    $("f-de").value = hojeISO();
    $("f-ate").value = hojeISO();
    carregar();
  });

  $("cancelar-venda").addEventListener("click", cancelarVenda);
  $("fechar-detalhe").addEventListener("click", () => $("detalhe").close());

  await carregar();
})();   