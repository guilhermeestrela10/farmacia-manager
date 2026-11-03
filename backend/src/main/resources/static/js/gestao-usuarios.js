let usuarios = [];
let temporizadorAviso;
let usuarioDaSenha = null;
let senhaPropria = false;

const PERFIS = { ADMIN: "Administrador", VENDEDOR: "Vendedor" };

function mostrarAviso(texto) {
  const aviso = $("aviso");
  aviso.textContent = texto;
  aviso.hidden = false;
  clearTimeout(temporizadorAviso);
  temporizadorAviso = setTimeout(() => { aviso.hidden = true; }, 6000);
}

/* ---------- Tabela ---------- */

function renderTabela() {
  const corpo = $("corpo");
  corpo.replaceChildren();
  $("vazio").hidden = usuarios.length > 0;

  for (const u of usuarios) {
    const nome = criar("td");
    nome.append(criar("strong", "", u.nome));

    const perfil = criar("td");
    perfil.append(criar("span", "selo " + (u.perfil === "ADMIN" ? "ok" : "neutro"),
      PERFIS[u.perfil] || u.perfil));

    const situacao = criar("td");
    situacao.append(criar("span", "selo " + (u.ativo ? "bom" : "off"),
      u.ativo ? "Ativo" : "Inativo"));

    const redefinir = criar("button", "btn sec", "Redefinir senha");
    redefinir.type = "button";
    redefinir.addEventListener("click", () => abrirSenha(u));

    const alternar = criar("button", "btn sec", u.ativo ? "Desativar" : "Reativar");
    alternar.type = "button";
    alternar.addEventListener("click", () => alternarSituacao(u));

    const acoes = criar("td", "acoes");
    acoes.append(redefinir, alternar);

    const linha = criar("tr", u.ativo ? "" : "inativo");
    linha.append(nome, criar("td", "", u.login), perfil, situacao, acoes);
    corpo.append(linha);
  }
}

async function carregar() {
  limparErro();
  try {
    usuarios = await requisitar("/usuarios");
    renderTabela();
  } catch (erro) {
    mostrarErro(erro.message);
  }
}

async function alternarSituacao(usuario) {
  if (usuario.ativo) {
    const confirmado = confirm(
      "Desativar " + usuario.nome + "?\n\nEssa pessoa perde o acesso ao sistema na hora. "
      + "O histórico dela é mantido.");
    if (!confirmado) return;
  }

  try {
    await enviar("PATCH",
      "/usuarios/" + usuario.id + (usuario.ativo ? "/desativar" : "/ativar"));
    mostrarAviso(usuario.ativo ? "Usuário desativado." : "Usuário reativado.");
    await carregar();
  } catch (erro) {
    mostrarErro(erro.message);
  }
}

/* ---------- Novo usuário ---------- */

function abrirFormulario() {
  $("nome").value = "";
  $("login").value = "";
  $("senha").value = "";
  $("senha").type = "password";
  $("ver-senha").checked = false;
  $("perfil").value = "VENDEDOR";
  $("erro-form").hidden = true;
  $("janela").showModal();
}

async function salvar(evento) {
  evento.preventDefault();

  const botao = $("salvar");
  botao.disabled = true;

  try {
    const criado = await enviar("POST", "/usuarios", {
      nome: $("nome").value.trim(),
      login: $("login").value.trim(),
      senha: $("senha").value,
      perfil: $("perfil").value,
    });

    $("senha").value = "";
    $("janela").close();
    mostrarAviso("Usuário " + criado.nome + " criado.");
    await carregar();
  } catch (erro) {
    $("erro-form").textContent = erro.message;
    $("erro-form").hidden = false;
  } finally {
    botao.disabled = false;
  }
}

/* ---------- Redefinir senha ---------- */

function abrirSenha(usuario) {
  usuarioDaSenha = usuario;
  senhaPropria = Boolean(window.usuarioAtual && window.usuarioAtual.login === usuario.login);

  $("titulo-senha").textContent = "Redefinir senha de " + usuario.nome;
  $("aviso-propria").hidden = !senhaPropria;
  $("nova-senha").value = "";
  $("nova-senha").type = "password";
  $("ver-nova-senha").checked = false;
  $("erro-senha").hidden = true;
  $("janela-senha").showModal();
}

async function salvarSenha(evento) {
  evento.preventDefault();

  const botao = $("salvar-senha");
  botao.disabled = true;

  try {
    await enviar("POST", "/usuarios/" + usuarioDaSenha.id + "/senha",
      { senha: $("nova-senha").value });

    $("nova-senha").value = "";
    $("janela-senha").close();

    if (senhaPropria) {
      location.href = "login.html";
      return;
    }

    mostrarAviso("Senha de " + usuarioDaSenha.nome
      + " redefinida. Se ela estava logada, foi desconectada.");
  } catch (erro) {
    $("erro-senha").textContent = erro.message;
    $("erro-senha").hidden = false;
  } finally {
    botao.disabled = false;
  }
}

/* ---------- Eventos ---------- */

$("novo").addEventListener("click", abrirFormulario);
$("cancelar").addEventListener("click", () => $("janela").close());
$("formulario").addEventListener("submit", salvar);
$("ver-senha").addEventListener("change", () => {
  $("senha").type = $("ver-senha").checked ? "text" : "password";
});

$("cancelar-senha").addEventListener("click", () => $("janela-senha").close());
$("form-senha").addEventListener("submit", salvarSenha);
$("ver-nova-senha").addEventListener("change", () => {
  $("nova-senha").type = $("ver-nova-senha").checked ? "text" : "password";
});

carregar();