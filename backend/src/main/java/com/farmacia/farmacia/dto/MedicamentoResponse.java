package com.farmacia.farmacia.dto;

import java.math.BigDecimal;

public record MedicamentoResponse(
        Long id,
        String nome,
        String codigoBarras,
        Long categoriaId,
        String categoriaNome,
        Long fabricanteId,
        String fabricanteNome,
        BigDecimal preco,
        int estoqueMinimo,
        boolean ativo
) {
}