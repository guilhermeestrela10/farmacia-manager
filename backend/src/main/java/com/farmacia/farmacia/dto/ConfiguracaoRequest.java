package com.farmacia.farmacia.dto;
import jakarta.validation.constraints.*;
public record ConfiguracaoRequest(@NotBlank @Size(max=100) String nomeFarmacia, @Size(max=30) String telefone, @Email @Size(max=150) String email, @Size(max=200) String endereco) {}
