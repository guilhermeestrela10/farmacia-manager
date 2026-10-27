package com.farmacia.farmacia.dto;

import java.math.BigDecimal;

public record ProdutoVendidoResponse(
        Long medicamentoId,
        String medicamento,
        long unidades,
        BigDecimal total
) {
}