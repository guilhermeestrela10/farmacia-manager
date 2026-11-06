package com.farmacia.farmacia.controller;

import com.farmacia.farmacia.model.Perfil;
import com.farmacia.farmacia.repository.UsuarioRepository;
import com.farmacia.farmacia.security.AuditoriaDeAcessos;
import com.farmacia.farmacia.security.UsuarioAtivoFilter;
import com.farmacia.farmacia.security.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Profile("demo")
public class DemoController {
    public record AcessoDemo(@NotNull Perfil perfil) {}
    private final UsuarioRepository usuarios;
    private final AuditoriaDeAcessos acessos;
    public DemoController(UsuarioRepository usuarios, AuditoriaDeAcessos acessos) {
        this.usuarios = usuarios;
        this.acessos = acessos;
    }

    @GetMapping("/demo/info")
    public Map<String, Object> informacoes() {
        return Map.of("demo", true, "aviso", "Demonstração com dados fictícios e compartilhados. Não use dados pessoais, receitas reais ou este ambiente para atendimento ou emissão fiscal.");
    }

    @PostMapping("/demo/entrar")
    public Map<String, String> entrar(@Valid @RequestBody AcessoDemo acesso,
            HttpServletRequest request, HttpServletResponse response) {
        String login = acesso.perfil() == Perfil.ADMIN ? "demo.admin" : "demo.vendedor";
        var usuario = usuarios.findByLogin(login).filter(u -> u.isAtivo() && u.getPerfil() == acesso.perfil())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Conta de demonstração indisponível."));
        var detalhes = new UsuarioAutenticado(usuario, false);
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(detalhes, null, detalhes.getAuthorities()));
        if (request.getSession(false) != null) request.changeSessionId();
        SecurityContextHolder.setContext(contexto);
        new HttpSessionSecurityContextRepository().saveContext(contexto, request, response);
        request.getSession().setAttribute(UsuarioAtivoFilter.ATRIBUTO_LOGIN, detalhes.getVersaoSenha());
        CookieCsrfTokenRepository.withHttpOnlyFalse().saveToken(null, request, response);
        acessos.registrar(request, login, "Acesso público de demonstração", 200);
        return Map.of("mensagem", "Acesso à demonstração autorizado.");
    }
}
