package com.farmacia.farmacia.dto;

import java.time.Instant;

import com.farmacia.farmacia.model.TipoMovimentacao;

public record MovimentacaoResponse(
        Long id,
        Long loteId,
        String numeroLote,
        Long medicamentoId,
        String medicamentoNome,
        TipoMovimentacao tipo,
        int quantidade,
        String motivo,
        String responsavel,
        Instant dataHora
) {
}