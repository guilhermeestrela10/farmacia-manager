package com.farmacia.farmacia.dto;

import com.farmacia.farmacia.model.TipoMovimentacao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AjusteEstoqueRequest(
        @NotNull(message = "O lote é obrigatório.")
        Long loteId,

        @NotNull(message = "O tipo é obrigatório (AJUSTE_ENTRADA ou AJUSTE_SAIDA).")
        TipoMovimentacao tipo,

        @NotNull(message = "A quantidade é obrigatória.")
        @Positive(message = "A quantidade deve ser maior que zero.")
        Integer quantidade,

        @NotBlank(message = "O motivo é obrigatório.")
        @Size(max = 200, message = "O motivo deve ter no máximo 200 caracteres.")
        String motivo,

        @NotBlank(message = "O responsável é obrigatório.")
        @Size(max = 100, message = "O responsável deve ter no máximo 100 caracteres.")
        String responsavel
) {
}