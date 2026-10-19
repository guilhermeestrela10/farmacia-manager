package com.farmacia.farmacia.dto;

import java.math.BigDecimal;
import java.util.List;

public record HistoricoVendasResponse(
        List<VendaResumoResponse> vendas,
        long quantidadeConcluidas,
        long quantidadeCanceladas,
        BigDecimal totalVendido,
        List<TotalFormaResponse> porForma,
        boolean truncado
) {
}