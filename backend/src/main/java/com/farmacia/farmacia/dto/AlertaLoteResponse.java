package com.farmacia.farmacia.dto;

import java.time.LocalDate;

public record AlertaLoteResponse(
        Long loteId,
        Long medicamentoId,
        String medicamentoNome,
        String numeroLote,
        LocalDate validade,
        int quantidadeAtual,
        long diasParaVencer
) {
}