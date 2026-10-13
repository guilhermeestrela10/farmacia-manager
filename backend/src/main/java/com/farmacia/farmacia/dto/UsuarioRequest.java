package com.farmacia.farmacia.dto;

import com.farmacia.farmacia.model.Perfil;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
        String nome,

        @NotBlank(message = "O login é obrigatório.")
        @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$",
                message = "O login deve ter de 3 a 50 caracteres: letras, números, ponto, hífen ou sublinhado.")
        String login,

        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres.")
        String senha,

        @NotNull(message = "O perfil é obrigatório (ADMIN ou VENDEDOR).")
        Perfil perfil
) {
}