package com.farmacia.farmacia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelamentoRequest(
        @NotBlank(message = "Informe o motivo do cancelamento.")
        @Size(max = 150, message = "O motivo deve ter no máximo 150 caracteres.")
        String motivo
) {
}