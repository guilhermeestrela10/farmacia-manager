const ICONES = {
  dashboard: '<rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/>',
  estoque: '<path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/>',
  pilula: '<g transform="rotate(-45 12 12)"><rect x="2" y="8" width="20" height="8" rx="4"/><line x1="12" y1="8" x2="12" y2="16"/></g>',
  calendario: '<rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>',
  movimentacoes: '<polyline points="17 1 21 5 17 9"/><path d="M3 11V9a4 4 0 0 1 4-4h14"/><polyline points="7 23 3 19 7 15"/><path d="M21 13v2a4 4 0 0 1-4 4H3"/>',
  clientes: '<path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>',
  atendimento: '<polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>',
  receitas: '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/>',
  dispensacoes: '<polyline points="9 11 12 14 22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>',
  relatorios: '<line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/>',
  usuarios: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>',
  auditoria: '<path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1" ry="1"/>',
  config: '<line x1="4" y1="21" x2="4" y2="14"/><line x1="4" y1="10" x2="4" y2="3"/><line x1="12" y1="21" x2="12" y2="12"/><line x1="12" y1="8" x2="12" y2="3"/><line x1="20" y1="21" x2="20" y2="16"/><line x1="20" y1="12" x2="20" y2="3"/><line x1="1" y1="14" x2="7" y2="14"/><line x1="9" y1="8" x2="15" y2="8"/><line x1="17" y1="16" x2="23" y2="16"/>',
  sair: '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/>',
  menu: '<line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="18" x2="21" y2="18"/>',
  alerta: '<path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>',
  queda: '<polyline points="23 18 13.5 8.5 8.5 13.5 1 6"/><polyline points="17 18 23 18 23 12"/>',
  camadas: '<polygon points="12 2 2 7 12 12 22 7 12 2"/><polyline points="2 17 12 22 22 17"/><polyline points="2 12 12 17 22 12"/>',
  mais: '<line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>',
};

const MENU = [
  { titulo: "Operação", itens: [
    ["dashboard", "Dashboard", "dashboard", "index.html"],
    ["estoque", "Estoque", "estoque", "estoque.html"],
    ["medicamentos", "Medicamentos", "pilula", "medicamentos.html"],
    ["lotes", "Lotes e Validades", "calendario", null],
    ["movimentacoes", "Movimentações", "movimentacoes", null],
  ] },
  { titulo: "Atendimento", itens: [
    ["clientes", "Clientes", "clientes", null],
    ["atendimento", "Atendimento Farmacêutico", "atendimento", null],
    ["receitas", "Receitas", "receitas", null],
    ["dispensacoes", "Dispensações", "dispensacoes", null],
  ] },
  { titulo: "Gestão", soAdmin: true, itens: [
    ["relatorios", "Relatórios", "relatorios", null],
    ["usuarios", "Usuários e Permissões", "usuarios", null],
    ["auditoria", "Auditoria", "auditoria", null],
    ["configuracoes", "Configurações", "config", null],
  ] },
];

function icone(nome, tamanho) {
  const lado = tamanho || 18;
  const envoltorio = criar("span", "icone");
  // O conteúdo vem das constantes acima, nunca de dados digitados.
  envoltorio.innerHTML = '<svg viewBox="0 0 24 24" width="' + lado + '" height="' + lado
    + '" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" '
    + 'stroke-linejoin="round" aria-hidden="true">' + ICONES[nome] + "</svg>";
  return envoltorio;
}

function itemDoMenu(item, pagina) {
  const [id, titulo, nomeIcone, endereco] = item;
  const classes = "item-menu" + (id === pagina ? " ativo" : "") + (endereco ? "" : " em-breve");
  const link = criar("a", classes);

  if (endereco) {
    link.href = endereco;
  } else {
    link.setAttribute("aria-disabled", "true");
  }

  link.append(icone(nomeIcone, 18), criar("span", "", titulo));
  if (!endereco) link.append(criar("small", "", "em breve"));
  return link;
}

function construirLateral(pagina) {
  const lateral = criar("aside", "barra-lateral");

  const marca = criar("div", "marca-lateral");
  const textos = criar("div");
  textos.append(criar("span", "marca-pequena", "Farmácias Ruanna"),
    criar("strong", "", "Farmácia Manager"));
  marca.append(criar("span", "capsula"), textos);
  lateral.append(marca);

  for (const secao of MENU) {
    const bloco = criar("section", "secao-menu");
    if (secao.soAdmin) {
      bloco.hidden = true;
      bloco.dataset.soAdmin = "true";
    }
    bloco.append(criar("h3", "", secao.titulo));
    for (const item of secao.itens) bloco.append(itemDoMenu(item, pagina));
    lateral.append(bloco);
  }

  lateral.append(criar("div", "rodape-lateral",
    "Organização • Controle • Segurança • Informação"));
  return lateral;
}

function construirTopo(shell) {
  const alternar = criar("button", "btn-icone");
  alternar.type = "button";
  alternar.title = "Mostrar ou esconder o menu";
  alternar.append(icone("menu", 20));
  alternar.addEventListener("click", () => shell.classList.toggle("lateral-fechada"));

  const usuario = criar("div", "usuario-topo");
  const nome = criar("strong", "", "");
  nome.id = "usuario-nome";
  const perfil = criar("small", "", "");
  perfil.id = "usuario-perfil";
  usuario.append(nome, perfil);

  const botaoSair = criar("button", "btn sec btn-sair");
  botaoSair.type = "button";
  botaoSair.append(icone("sair", 16), criar("span", "", "Sair"));
  botaoSair.addEventListener("click", sair);

  const topo = criar("header", "barra-topo");
  topo.append(alternar,
    criar("span", "slogan", "Farmácias Ruanna · A saúde da comunidade"),
    criar("span", "espaco"), usuario, botaoSair);
  return topo;
}

async function carregarUsuario() {
  try {
    const eu = await requisitar("/auth/me");
    window.usuarioAtual = eu;
    $("usuario-nome").textContent = eu.nome;
    $("usuario-perfil").textContent = eu.perfil === "ADMIN" ? "Administrador" : "Vendedor";

    if (eu.perfil === "ADMIN") {
      for (const secao of document.querySelectorAll("[data-so-admin]")) secao.hidden = false;
    }
  } catch (erro) {
    // sem sessão, requisitar() já redireciona para a tela de entrada
  }
}

function construirLayout() {
  const conteudo = document.querySelector("main");
  if (!conteudo) return;

  // Se alguma página ainda tiver o menu da versão antiga, ele é removido aqui.
  for (const antigo of document.querySelectorAll("aside.menu")) antigo.remove();

  const shell = criar("div", "shell");
  const coluna = criar("div", "coluna");
  coluna.append(construirTopo(shell), conteudo);
  shell.append(construirLateral(document.body.dataset.pagina), coluna);
  document.body.prepend(shell);

  if (window.matchMedia("(max-width: 800px)").matches) {
    shell.classList.add("lateral-fechada");
  }

  carregarUsuario();
}

construirLayout();