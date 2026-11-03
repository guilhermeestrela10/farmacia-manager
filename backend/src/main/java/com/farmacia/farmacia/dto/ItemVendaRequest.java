package com.farmacia.farmacia.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

public record ItemVendaRequest(
        @NotNull(message = "O medicamento é obrigatório.")
        Long medicamentoId,

        @NotNull(message = "A quantidade é obrigatória.")
        @Positive(message = "A quantidade deve ser maior que zero.")
        @Max(value = 9999, message = "A quantidade máxima por item é 9999.")
        Integer quantidade,

        @Positive(message = "O preço esperado deve ser maior que zero.")
        @Digits(integer = 8, fraction = 2, message = "Preço esperado inválido.")
        BigDecimal precoEsperado
) {
}
