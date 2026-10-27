package com.farmacia.farmacia.dto;

import java.math.BigDecimal;

public record RelatorioEstoqueResponse(
        long unidadesDisponiveis,
        BigDecimal valorDisponivel,
        int diasVencimento,
        long unidadesVencendo,
        BigDecimal valorVencendo,
        long unidadesVencidas,
        BigDecimal valorVencido,
        long unidadesPerdidas,
        BigDecimal valorPerdas
) {
}