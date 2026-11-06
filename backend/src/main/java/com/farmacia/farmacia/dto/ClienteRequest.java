package com.farmacia.farmacia.dto;
import jakarta.validation.constraints.*;
public record ClienteRequest(@NotBlank @Size(max=100) String nome, @Size(max=30) String telefone, @Email @Size(max=150) String email) {}
