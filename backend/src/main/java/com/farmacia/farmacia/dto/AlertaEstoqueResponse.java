package com.farmacia.farmacia.dto;

public record AlertaEstoqueResponse(
        Long medicamentoId,
        String medicamentoNome,
        long saldoDisponivel,
        int estoqueMinimo
) {
}