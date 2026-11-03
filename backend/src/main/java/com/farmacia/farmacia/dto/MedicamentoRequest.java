package com.farmacia.farmacia.dto;

import java.math.BigDecimal;

import com.farmacia.farmacia.model.TipoMedicamento;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MedicamentoRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
        String nome,

        @Size(max = 20, message = "O código de barras deve ter no máximo 20 caracteres.")
        String codigoBarras,

        @NotNull(message = "A categoria é obrigatória.")
        Long categoriaId,

        @NotNull(message = "O fabricante é obrigatório.")
        Long fabricanteId,

        @NotNull(message = "O tipo é obrigatório (Referência, Genérico ou Similar).")
        TipoMedicamento tipo,

        @NotNull(message = "O preço é obrigatório.")
        @Positive(message = "O preço deve ser maior que zero.")
        @Digits(integer = 8, fraction = 2, message = "O preço deve ter no máximo 8 dígitos inteiros e 2 casas decimais.")
        BigDecimal preco,

        @NotNull(message = "O estoque mínimo é obrigatório.")
        @Min(value = 0, message = "O estoque mínimo não pode ser negativo.")
        Integer estoqueMinimo
) {
}
