package com.farmacia.farmacia.dto;

import java.math.BigDecimal;

public record VendedorTotalResponse(
        String vendedor,
        long quantidade,
        BigDecimal total
) {
}   