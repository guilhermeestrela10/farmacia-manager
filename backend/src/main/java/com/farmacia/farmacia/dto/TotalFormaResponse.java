package com.farmacia.farmacia.dto;

import java.math.BigDecimal;

import com.farmacia.farmacia.model.FormaPagamento;

public record TotalFormaResponse(
        FormaPagamento forma,
        BigDecimal total,
        long quantidade
) {
}