package com.farmacia.farmacia.dto;

import java.math.BigDecimal;

import com.farmacia.farmacia.model.TipoMedicamento;

public record MedicamentoResponse(
        Long id,
        String nome,
        String codigoBarras,
        Long categoriaId,
        String categoriaNome,
        Long fabricanteId,
        String fabricanteNome,
        TipoMedicamento tipo,
        BigDecimal preco,
        int estoqueMinimo,
        boolean ativo
) {
}