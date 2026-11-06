(() => {
  const campos = [['nome-farmacia', 'nomeFarmacia'], ['telefone', 'telefone'], ['email', 'email'], ['endereco', 'endereco']];
  R.formulario('formulario', async () => {
    const corpo = Object.fromEntries(campos.map(([id, chave]) => [chave, $(id).value.trim()]));
    await enviar('PUT', '/gestao/configuracoes', corpo);
    if (typeof atualizarNomeFarmacia === 'function') atualizarNomeFarmacia(corpo);
    R.aviso('Configurações salvas.');
  });
  R.carregar('configuracoes', () => Promise.all([requisitar('/auth/me'), requisitar('/configuracoes')]), ([usuario, configuracoes]) => {
    for (const [id, chave] of campos) $(id).value = configuracoes[chave] || '';
    if (usuario.perfil === 'ADMIN') $('formulario').querySelector('fieldset').disabled = false;
    else mostrarErro('Somente administradores podem alterar as configurações.');
  });
})();
