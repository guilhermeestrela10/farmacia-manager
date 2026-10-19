package com.farmacia.farmacia.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.farmacia.farmacia.model.FormaPagamento;
import com.farmacia.farmacia.model.StatusVenda;

public record VendaResumoResponse(
        Long id,
        Instant criadaEm,
        String vendedor,
        FormaPagamento formaPagamento,
        BigDecimal total,
        StatusVenda status,
        String motivoCancelamento
) {
}