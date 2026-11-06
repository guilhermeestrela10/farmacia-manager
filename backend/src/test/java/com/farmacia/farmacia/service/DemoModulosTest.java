package com.farmacia.farmacia.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.farmacia.farmacia.dto.*;
import com.farmacia.farmacia.model.*;
import com.farmacia.farmacia.repository.*;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class DemoModulosTest {
    @Test void rejeitaDatasIncoerentesAntesDeSalvar() {
        var receitas = mock(ReceitaRepository.class);
        var service = new ReceitaService(receitas, mock(ClienteService.class), mock(MedicamentoRepository.class), mock(DispensacaoRepository.class));
        var request = new ReceitaRequest(1L, "Fictício", "EXEMPLO", LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), null, List.of(new ItemReceitaRequest(1L, 1)));
        assertThrows(ResponseStatusException.class, () -> service.criar(request));
        verifyNoInteractions(receitas);
    }
    @Test void clienteInativoNaoPodeSerAtendido() {
        var clientes = mock(ClienteRepository.class);
        var cliente = new Cliente("Exemplo", null, null);
        cliente.definirAtivo(false);
        when(clientes.buscarPorIdComTravamento(1L)).thenReturn(java.util.Optional.of(cliente));
        var service = new ClienteService(clientes);
        assertThrows(ResponseStatusException.class, () -> service.buscarAtivo(1L));
    }
    @Test void receitaVencidaNaoCriaVenda() {
        var receitas = mock(ReceitaRepository.class);
        var vendas = mock(VendaService.class);
        var receita = new Receita(new Cliente("Exemplo", null, null), "Exemplo", "1", LocalDate.now().minusDays(2), LocalDate.now().minusDays(1), null);
        when(receitas.buscarPorIdComTravamento(1L)).thenReturn(java.util.Optional.of(receita));
        var service = new DispensacaoService(mock(DispensacaoRepository.class), receitas, mock(ClienteService.class), vendas, mock(VendaRepository.class));
        assertThrows(ResponseStatusException.class, () -> service.registrar(new DispensacaoRequest(1L, FormaPagamento.PIX, List.of(new ItemVendaRequest(1L, 1, null))), "demo"));
        verifyNoInteractions(vendas);
    }
}
