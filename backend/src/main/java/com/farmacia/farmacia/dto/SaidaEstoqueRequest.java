package com.farmacia.farmacia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SaidaEstoqueRequest(
        @NotNull(message = "O medicamento é obrigatório.")
        Long medicamentoId,

        @NotNull(message = "A quantidade é obrigatória.")
        @Positive(message = "A quantidade deve ser maior que zero.")
        Integer quantidade,

        @Size(max = 200, message = "O motivo deve ter no máximo 200 caracteres.")
        String motivo,

        @NotBlank(message = "O responsável é obrigatório.")
        @Size(max = 100, message = "O responsável deve ter no máximo 100 caracteres.")
        String responsavel
) {
}