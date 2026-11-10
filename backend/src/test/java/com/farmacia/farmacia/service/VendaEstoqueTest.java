package com.farmacia.farmacia.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.farmacia.farmacia.dto.*;
import com.farmacia.farmacia.model.*;
import com.farmacia.farmacia.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class VendaEstoqueTest {
    private final VendaRepository vendas = mock(VendaRepository.class);
    private final MedicamentoRepository medicamentos = mock(MedicamentoRepository.class);
    private final LoteRepository lotes = mock(LoteRepository.class);
    private final MovimentacaoEstoqueRepository movimentos = mock(MovimentacaoEstoqueRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final VendaService service = new VendaService(vendas, medicamentos, lotes, movimentos, usuarios, mock(MedicamentoService.class));

    private Medicamento medicamento(String preco) {
        var med = new Medicamento("Fictício", null, new Categoria("Teste"), new Fabricante("Teste"), TipoMedicamento.GENERICO, new BigDecimal(preco), 0);
        ReflectionTestUtils.setField(med, "id", 1L);
        when(medicamentos.buscarPorIdComTravamento(1L)).thenReturn(Optional.of(med));
        when(usuarios.findByLogin("vendedor")).thenReturn(Optional.of(new Usuario("Teste", "vendedor", "hash-ficticio", Perfil.VENDEDOR)));
        return med;
    }

    @Test
    void rejeitaPrecoDiferenteDoConfirmadoAntesDeGravarVenda() {
        medicamento("20.00");
        var request = new VendaRequest(List.of(new ItemVendaRequest(1L, 1, new BigDecimal("10.00"))), FormaPagamento.DINHEIRO);
        var erro = assertThrows(ResponseStatusException.class, () -> service.registrar(request, "vendedor"));
        assertEquals(409, erro.getStatusCode().value());
        verifyNoInteractions(vendas, lotes, movimentos);
    }

    @Test
    void rejeitaTotalQueNaoCabeNoBancoAntesDeGravar() {
        medicamento("99999999.99");
        var request = new VendaRequest(List.of(new ItemVendaRequest(1L, 9999, null)), FormaPagamento.PIX);
        var erro = assertThrows(ResponseStatusException.class, () -> service.registrar(request, "vendedor"));
        assertEquals(409, erro.getStatusCode().value());
        verifyNoInteractions(vendas, lotes, movimentos);
    }

    @Test
    void saldoDisponivelNaoEstouraQuandoSomaVariosLotesGrandes() {
        var med = medicamento("1.00");
        var lote1 = new Lote(med, "1", LocalDate.now().plusDays(2));
        var lote2 = new Lote(med, "2", LocalDate.now().plusDays(3));
        lote1.adicionar(Integer.MAX_VALUE);
        lote2.adicionar(Integer.MAX_VALUE);
        when(lotes.buscarDisponiveisParaSaida(eq(1L), any())).thenReturn(List.of(lote1, lote2));
        when(vendas.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        service.registrar(new VendaRequest(List.of(new ItemVendaRequest(1L, 1, null)), FormaPagamento.PIX), "vendedor");
        assertEquals(Integer.MAX_VALUE - 1, lote1.getQuantidadeAtual());
        assertEquals(Integer.MAX_VALUE, lote2.getQuantidadeAtual());
        verify(lotes).save(lote1);
        verify(lotes, never()).save(lote2);
    }

    @Test
    void loteNaoAceitaOverflowNemQuantidadesNegativas() {
        var lote = new Lote(medicamento("1.00"), "1", LocalDate.now().plusDays(2));
        lote.adicionar(Integer.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> lote.adicionar(1));
        assertEquals(Integer.MAX_VALUE, lote.getQuantidadeAtual());
        assertThrows(IllegalArgumentException.class, () -> lote.adicionar(-1));
        assertThrows(IllegalArgumentException.class, () -> lote.remover(-1));
    }

    @Test
    void loteVencidoNaoPodeSerBaixadoPorAjuste() {
        var lote = new Lote(medicamento("1.00"), "Vencido", LocalDate.now().minusDays(1));
        lote.adicionar(5);
        when(lotes.buscarPorIdComTravamento(1L)).thenReturn(Optional.of(lote));
        var estoque = new EstoqueService(medicamentos, lotes, movimentos);
        var erro = assertThrows(ResponseStatusException.class, () -> estoque.registrarAjuste(
                new AjusteEstoqueRequest(1L, TipoMovimentacao.AJUSTE_SAIDA, 1, "Teste", "Teste")));
        assertEquals(409, erro.getStatusCode().value());
        assertEquals(5, lote.getQuantidadeAtual());
        verifyNoInteractions(movimentos);
    }
}
