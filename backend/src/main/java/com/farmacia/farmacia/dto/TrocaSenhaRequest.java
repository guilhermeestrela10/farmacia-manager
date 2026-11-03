package com.farmacia.farmacia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrocaSenhaRequest(
        @NotBlank(message = "Informe a senha atual.")
        @Size(max = 72, message = "A senha atual está incorreta.")
        String senhaAtual,

        @NotBlank(message = "A nova senha é obrigatória.")
        @Size(min = 8, max = 72, message = "A nova senha deve ter de 8 a 72 caracteres.")
        String novaSenha
) {
}