const CAMPOS = ["atual", "nova", "confirmar"];

async function trocar(evento) {
  evento.preventDefault();
  limparErro();

  const nova = $("nova").value;

  if (nova !== $("confirmar").value) {
    mostrarErro("A repetição não confere com a nova senha.");
    return;
  }

  const botao = $("trocar");
  botao.disabled = true;

  try {
    await enviar("POST", "/auth/senha", {
      senhaAtual: $("atual").value,
      novaSenha: nova,
    });

    for (const id of CAMPOS) $(id).value = "";
    $("formulario").hidden = true;
    $("sucesso").hidden = false;
    setTimeout(() => { location.href = "login.html"; }, 2500);
  } catch (erro) {
    mostrarErro(erro.message);
    $("atual").value = "";
    $("atual").focus();
  } finally {
    botao.disabled = false;
  }
}

$("formulario").addEventListener("submit", trocar);
$("ver-senhas").addEventListener("change", () => {
  const tipo = $("ver-senhas").checked ? "text" : "password";
  for (const id of CAMPOS) $(id).type = tipo;
});