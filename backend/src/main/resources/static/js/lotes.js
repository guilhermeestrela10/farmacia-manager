(() => {
  function carregar() {
    const parametros = new URLSearchParams({busca: $('busca').value.trim(), situacao: $('situacao').value});
    return R.carregar('lotes', () => requisitar('/estoque/lotes?' + parametros), lista => {
      R.tabela('corpo', lista.map(lote => {
        const link = criar('a', 'btn sec', 'Abrir estoque');
        link.href = 'estoque.html?medicamentoId=' + encodeURIComponent(lote.medicamentoId);
        return [lote.medicamentoNome, lote.numeroLote, dataBR(lote.validade), lote.quantidadeAtual, link];
      }));
    });
  }
  $('filtros').addEventListener('submit', evento => {evento.preventDefault(); carregar();});
  $('situacao').addEventListener('change', carregar);
  carregar();
})();
