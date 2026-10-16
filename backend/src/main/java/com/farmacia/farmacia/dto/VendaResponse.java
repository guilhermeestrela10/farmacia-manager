package com.farmacia.farmacia.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.farmacia.farmacia.model.FormaPagamento;
import com.farmacia.farmacia.model.StatusVenda;

public record VendaResponse(
        Long id,
        Instant criadaEm,
        String vendedor,
        FormaPagamento formaPagamento,
        BigDecimal total,
        StatusVenda status,
        List<ItemVendaResponse> itens
) {
}