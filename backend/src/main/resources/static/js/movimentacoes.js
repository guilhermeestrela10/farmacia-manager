const LIMITE = 200;

const TIPOS = {
  ENTRADA: ["Entrada", "bom", "+"],
  SAIDA: ["Saída", "vencido", "−"],
  AJUSTE_ENTRADA: ["Ajuste (+)", "atencao", "+"],
  AJUSTE_SAIDA: ["Ajuste (−)", "atencao", "−"],
  PERDA: ["Perda", "vencido", "−"],
};

function dataHoraCompleta(iso) {
  return new Date(iso).toLocaleString("pt-BR",
    { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" });
}

function renderTabela(lista) {
  const corpo = $("corpo");
  corpo.replaceChildren();
  $("vazio").hidden = lista.length > 0;

  for (const m of lista) {
    const [rotulo, classe, sinal] = TIPOS[m.tipo] || [m.tipo, "neutro", ""];

    const tipo = criar("td");
    tipo.append(criar("span", "selo " + classe, rotulo));

    const linha = criar("tr");
    linha.append(
      criar("td", "", dataHoraCompleta(m.dataHora)),
      criar("td", "", m.medicamentoNome),
      criar("td", "", m.numeroLote),
      tipo,
      criar("td", "num", sinal + m.quantidade),
      criar("td", "", m.motivo || "—"),
      criar("td", "", m.responsavel));

    corpo.append(linha);
  }

  if (lista.length >= LIMITE) {
    $("nota").textContent = "Mostrando as " + LIMITE
      + " movimentações mais recentes. Use os filtros para refinar.";
  } else {
    $("nota").textContent = lista.length + (lista.length === 1 ? " movimentação." : " movimentações.");
  }
}

async function carregar() {
  limparErro();

  const parametros = new URLSearchParams({ limite: String(LIMITE) });
  if ($("f-medicamento").value) parametros.set("medicamentoId", $("f-medicamento").value);
  if ($("f-tipo").value) parametros.set("tipo", $("f-tipo").value);
  if ($("f-de").value) parametros.set("de", $("f-de").value);
  if ($("f-ate").value) parametros.set("ate", $("f-ate").value);

  try {
    renderTabela(await requisitar("/estoque/movimentacoes?" + parametros));
  } catch (erro) {
    mostrarErro(erro.message);
  }
}

function limpar() {
  $("f-medicamento").value = "";
  $("f-tipo").value = "";
  $("f-de").value = "";
  $("f-ate").value = "";
  carregar();
}

async function iniciar() {
  const select = $("f-medicamento");
  const todos = criar("option", "", "Todos os medicamentos");
  todos.value = "";
  select.append(todos);

  try {
    const medicamentos = await requisitar("/medicamentos?incluirInativos=true");
    for (const med of medicamentos) {
      const opcao = criar("option", "", med.nome + (med.ativo ? "" : " (inativo)"));
      opcao.value = String(med.id);
      select.append(opcao);
    }
  } catch (erro) {
    mostrarErro(erro.message);
  }

  const parametros = new URLSearchParams(location.search);
  const tipo = parametros.get("tipo");
  if (tipo && TIPOS[tipo]) $("f-tipo").value = tipo;
  const medicamentoId = parametros.get("medicamentoId");
  if (medicamentoId) select.value = medicamentoId;

  for (const id of ["f-medicamento", "f-tipo", "f-de", "f-ate"]) {
    $(id).addEventListener("change", carregar);
  }
  $("limpar").addEventListener("click", limpar);

  await carregar();
}

iniciar();