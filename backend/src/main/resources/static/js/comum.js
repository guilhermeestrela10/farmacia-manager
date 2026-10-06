const $ = (id) => document.getElementById(id);

async function requisitar(caminho, opcoes) {
  let resposta;
  try {
    resposta = await fetch(caminho, opcoes);
  } catch (erro) {
    throw new Error("Não foi possível falar com o servidor. A aplicação está rodando?");
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