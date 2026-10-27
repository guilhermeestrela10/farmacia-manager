package com.farmacia.farmacia.dto;

import java.math.BigDecimal;

public record CategoriaTotalResponse(
        String categoria,
        long unidades,
        BigDecimal total
) {
}