package com.farmacia.farmacia.dto;

import java.math.BigDecimal;

public record ItemVendaResponse(
        Long medicamentoId,
        String medicamentoNome,
        int quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal
) {
}