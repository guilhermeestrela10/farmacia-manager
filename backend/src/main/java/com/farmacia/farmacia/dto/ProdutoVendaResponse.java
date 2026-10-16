package com.farmacia.farmacia.dto;

import java.math.BigDecimal;

import com.farmacia.farmacia.model.TipoMedicamento;

public record ProdutoVendaResponse(
        Long id,
        String nome,
        String categoriaNome,
        String fabricanteNome,
        TipoMedicamento tipo,
        BigDecimal preco,
        long disponivel
) {
}