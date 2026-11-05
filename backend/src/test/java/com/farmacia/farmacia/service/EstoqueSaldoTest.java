package com.farmacia.farmacia.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.farmacia.farmacia.model.*;
import com.farmacia.farmacia.repository.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EstoqueSaldoTest {
    @Test
    void saldoTotalEDisponivelVemDaMesmaConsultaSemLimitarAInteiro() {
        var medicamentos = mock(MedicamentoRepository.class);
        var lotes = mock(LoteRepository.class);
        var medicamento = new Medicamento("Fictício", null, new Categoria("Teste"),
                new Fabricante("Teste"), TipoMedicamento.GENERICO, BigDecimal.ONE, 2);
        ReflectionTestUtils.setField(medicamento, "id", 1L);
        when(medicamentos.findById(1L)).thenReturn(Optional.of(medicamento));
        when(lotes.resumirSaldoPorMedicamento(eq(1L), any()))
                .thenReturn(List.<Object[]>of(new Object[] {4_294_967_294L, 2_147_483_647L}));

        var saldo = new EstoqueService(medicamentos, lotes, mock(MovimentacaoEstoqueRepository.class))
                .consultarSaldo(1L);

        assertEquals(4_294_967_294L, saldo.saldoTotal());
        assertEquals(2_147_483_647L, saldo.saldoDisponivel());
        assertEquals(2_147_483_647L, saldo.saldoVencido());
        assertFalse(saldo.estoqueBaixo());
        verify(lotes).resumirSaldoPorMedicamento(eq(1L), any());
        verifyNoMoreInteractions(lotes);
    }
}
