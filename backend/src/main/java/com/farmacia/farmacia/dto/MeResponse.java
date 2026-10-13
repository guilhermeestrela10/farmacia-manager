package com.farmacia.farmacia.dto;

import com.farmacia.farmacia.model.Perfil;

public record MeResponse(String nome, String login, Perfil perfil) {
}