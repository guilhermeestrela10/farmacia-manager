package com.farmacia.farmacia.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RelatorioVendasResponse(
        LocalDate de,
        LocalDate ate,
        BigDecimal totalVendido,
        long quantidadeVendas,
        BigDecimal ticketMedio,
        long unidadesVendidas,
        long quantidadeCanceladas,
        BigDecimal valorCancelado,
        List<VendaDiaResponse> porDia,
        List<TotalFormaResponse> porForma,
        List<VendedorTotalResponse> porVendedor,
        List<CategoriaTotalResponse> porCategoria,
        List<ProdutoVendidoResponse> maisVendidos
) {
}