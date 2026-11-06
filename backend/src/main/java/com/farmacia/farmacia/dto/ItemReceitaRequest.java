package com.farmacia.farmacia.dto;
import jakarta.validation.constraints.*;
public record ItemReceitaRequest(@NotNull Long medicamentoId, @NotNull @Min(1) @Max(9999) Integer quantidade) {}
