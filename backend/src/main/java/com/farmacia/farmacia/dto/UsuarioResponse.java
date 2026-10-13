package com.farmacia.farmacia.dto;

import com.farmacia.farmacia.model.Perfil;

public record UsuarioResponse(
        Long id,
        String nome,
        String login,
        Perfil perfil,
        boolean ativo
) {
}