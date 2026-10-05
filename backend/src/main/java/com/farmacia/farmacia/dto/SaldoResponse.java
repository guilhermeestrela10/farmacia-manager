package com.farmacia.farmacia.dto;

public record SaldoResponse(
        Long medicamentoId,
        String medicamentoNome,
        long saldoTotal,
        long saldoDisponivel,
        long saldoVencido,
        int estoqueMinimo,
        boolean estoqueBaixo
) {
}