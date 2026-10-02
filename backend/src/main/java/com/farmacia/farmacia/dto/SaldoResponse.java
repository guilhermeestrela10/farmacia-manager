package com.farmacia.farmacia.dto;

public record SaldoResponse(
        Long medicamentoId,
        String medicamentoNome,
        long saldoTotal,
        int estoqueMinimo,
        boolean estoqueBaixo
) {
}