package com.farmacia.farmacia.dto;
import jakarta.validation.constraints.*;
public record AtendimentoRequest(@NotNull Long clienteId, @NotBlank @Size(max=100) String profissional, @NotBlank @Size(max=2000) String observacoes) {}
