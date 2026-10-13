const $ = (id) => document.getElementById(id);

function lerCookie(nome) {
  const par = document.cookie.split("; ").find((c) => c.startsWith(nome + "="));
  return par ? decodeURIComponent(par.split("=")[1]) : "";
}

async function requisitar(caminho, opcoes) {
  const configuracao = { ...(opcoes || {}) };
  const metodo = (configuracao.method || "GET").toUpperCase();
  configuracao.headers = { ...(configuracao.headers || {}) };

  if (metodo !== "GET" && metodo !== "HEAD") {
    configuracao.headers["X-XSRF-TOKEN"] = lerCookie("XSRF-TOKEN");
  }

  let resposta;
  try {
    resposta = await fetch(caminho, configuracao);
  } catch (erro) {
    throw new Error("Não foi possível falar com o servidor. A aplicação está rodando?");
  }

  if (resposta.status === 401 && !location.pathname.endsWith("login.html")) {
    location.href = "login.html";
    throw new Error("Sessão expirada. Entre novamente.");
  }

  if (!resposta.ok) {
    let mensagem = "Erro " + resposta.status + ".";
    try {
      const corpo = await resposta.json();
      if (corpo.mensagem) mensagem = corpo.mensagem;
    } catch (erro) {
      // resposta sem JSON: mantém a mensagem padrão
    }
    throw new Error(mensagem);
  }

  return resposta.status === 204 ? null : resposta.json();
}

function enviar(metodo, caminho, corpo) {
  const opcoes = { method: metodo };
  if (corpo !== undefined) {
    opcoes.headers = { "Content-Type": "application/json" };
    opcoes.body = JSON.stringify(corpo);
  }
  return requisitar(caminho, opcoes);
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

function moeda(valor) {
  return Number(valor).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

function mostrarErro(mensagem) {
  const caixa = $("erro");
  caixa.textContent = mensagem;
  caixa.hidden = false;
}

function limparErro() {
  $("erro").hidden = true;
}

async function sair() {
  try {
    await fetch("/logout", {
      method: "POST",
      headers: { "X-XSRF-TOKEN": lerCookie("XSRF-TOKEN") },
    });
  } catch (erro) {
    // mesmo sem resposta, volta para a tela de entrada
  }
  location.href = "login.html";
}

async function mostrarUsuario() {
  const menu = document.querySelector(".menu");
  if (!menu) return;

  try {
    const eu = await requisitar("/auth/me");
    window.usuarioAtual = eu;

    const caixa = criar("div");
    Object.assign(caixa.style, {
      padding: "16px",
      marginTop: "24px",
      borderTop: "1px solid rgba(255, 255, 255, .4)",
      fontSize: "13px",
    });

    const botao = criar("button", "", "Sair");
    botao.type = "button";
    Object.assign(botao.style, {
      marginTop: "8px",
      padding: "6px 14px",
      border: "1px solid #fff",
      borderRadius: "8px",
      background: "transparent",
      color: "#fff",
      cursor: "pointer",
    });
    botao.addEventListener("click", sair);

    const perfil = eu.perfil === "ADMIN" ? "Administrador" : "Vendedor";
    caixa.append(criar("strong", "", eu.nome), criar("div", "", perfil), botao);
    menu.append(caixa);
  } catch (erro) {
    // sem sessão, requisitar() já redireciona para a tela de entrada
  }
}

document.addEventListener("DOMContentLoaded", mostrarUsuario);