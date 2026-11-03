async function carregarAuditoria(evento) {
  if (evento) evento.preventDefault();
  if ($("buscar").disabled) return;
  limparErro();
  $("buscar").disabled = true;
  try {
    const parametros = new URLSearchParams();
    for (const id of ["de", "ate", "usuario", "tipo", "limite"]) {
      const valor = $(id).value.trim();
      if (valor) parametros.set(id, valor);
    }
    parametros.set("somenteRecusadas", $("recusadas").checked);
    const registros = await requisitar("/gestao/auditoria?" + parametros);
    $("registros").replaceChildren();
    const tipos = { ACESSO: "Acesso", ALTERACAO: "Alteração", PRECO: "Preço" };
    for (const registro of registros) {
      const linha = criar("tr");
      const resultado = registro.status >= 500 ? "Erro" : registro.status >= 400 ? "Recusado" : "Concluído";
      for (const valor of [new Date(registro.dataHora).toLocaleString("pt-BR"), registro.usuario,
        tipos[registro.tipo] || registro.tipo, registro.acao, resultado + " (" + registro.status + ")", registro.ip]) {
        linha.append(criar("td", "", valor));
      }
      $("registros").append(linha);
    }
    $("resumo").textContent = registros.length ? registros.length + " registro(s). Os mais recentes aparecem primeiro." : "Nenhum registro encontrado.";
  } catch (erro) {
    mostrarErro(erro.message);
  } finally {
    $("buscar").disabled = false;
  }
}

$("filtros").addEventListener("submit", carregarAuditoria);
carregarAuditoria();
