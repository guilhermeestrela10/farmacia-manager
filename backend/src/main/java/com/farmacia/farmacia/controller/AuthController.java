package com.farmacia.farmacia.controller;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.farmacia.farmacia.dto.MeResponse;
import com.farmacia.farmacia.dto.TrocaSenhaRequest;
import com.farmacia.farmacia.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

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

    @PostMapping("/auth/senha")
    public Map<String, String> trocarSenha(@Valid @RequestBody TrocaSenhaRequest request,
                                           Authentication autenticacao,
                                           HttpServletRequest requisicao) {
        service.trocarPropriaSenha(
                autenticacao.getName(), request.senhaAtual(), request.novaSenha());

        // A sessão atual termina: a pessoa entra de novo com a senha nova.
        HttpSession sessao = requisicao.getSession(false);
        if (sessao != null) {
            sessao.invalidate();
        }
        SecurityContextHolder.clearContext();

        return Map.of("mensagem", "Senha alterada. Entre novamente com a nova senha.");
    }
}