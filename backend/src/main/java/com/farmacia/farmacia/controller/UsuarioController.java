package com.farmacia.farmacia.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.farmacia.farmacia.dto.UsuarioRequest;
import com.farmacia.farmacia.dto.UsuarioResponse;
import com.farmacia.farmacia.service.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@Valid @RequestBody UsuarioRequest request) {
        return service.criar(request);
    }

    @PatchMapping("/{id}/desativar")
    public UsuarioResponse desativar(@PathVariable Long id, Authentication autenticacao) {
        return service.desativar(id, autenticacao.getName());
    }

    @PatchMapping("/{id}/ativar")
    public UsuarioResponse ativar(@PathVariable Long id) {
        return service.ativar(id);
    }
}