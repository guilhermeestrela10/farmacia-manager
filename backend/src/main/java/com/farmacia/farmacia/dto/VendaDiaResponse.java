package com.farmacia.farmacia.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VendaDiaResponse(
        LocalDate data,
        long quantidade,
        BigDecimal total
) {
}