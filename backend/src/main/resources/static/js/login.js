async function jaEstaLogado() {
  try {
    const resposta = await fetch("/auth/me");
    if (resposta.ok) location.href = "index.html";
  } catch (erro) {
    // servidor indisponível: o formulário mostra o erro ao tentar entrar
  }
}

async function entrar(evento) {
  evento.preventDefault();

  const botao = $("entrar");
  const caixa = $("erro");
  caixa.hidden = true;
  botao.disabled = true;

  try {
    const corpo = new URLSearchParams({
      username: $("login").value.trim(),
      password: $("senha").value,
    });

    const resposta = await fetch("/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        "X-XSRF-TOKEN": lerCookie("XSRF-TOKEN"),
      },
      body: corpo,
    });

    if (resposta.ok) {
      location.href = "index.html";
      return;
    }

    let mensagem = "Não foi possível entrar.";
    try {
      const dados = await resposta.json();
      if (dados.mensagem) mensagem = dados.mensagem;
    } catch (erro) {
      // resposta sem JSON: mantém a mensagem padrão
    }

    caixa.textContent = mensagem;
    caixa.hidden = false;
    $("senha").value = "";
    $("senha").focus();
  } catch (erro) {
    caixa.textContent = "Não foi possível falar com o servidor. A aplicação está rodando?";
    caixa.hidden = false;
  } finally {
    botao.disabled = false;
  }
}

$("formulario").addEventListener("submit", entrar);
jaEstaLogado();