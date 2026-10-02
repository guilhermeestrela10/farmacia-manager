package com.farmacia.farmacia.dto;

import java.time.LocalDate;

public record LoteResponse(
        Long id,
        Long medicamentoId,
        String medicamentoNome,
        String numeroLote,
        LocalDate validade,
        int quantidadeAtual
) {
}