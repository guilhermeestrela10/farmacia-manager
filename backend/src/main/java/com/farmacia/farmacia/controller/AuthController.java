package com.farmacia.farmacia.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.farmacia.farmacia.dto.MeResponse;
import com.farmacia.farmacia.service.UsuarioService;

@RestController
public class AuthController {

    private final UsuarioService service;

    public AuthController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping("/auth/me")
    public MeResponse eu(Authentication autenticacao) {
        return service.buscarPorLogin(autenticacao.getName());
    }
}