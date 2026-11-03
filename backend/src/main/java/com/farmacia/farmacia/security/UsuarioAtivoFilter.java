package com.farmacia.farmacia.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.farmacia.farmacia.model.Usuario;
import com.farmacia.farmacia.repository.UsuarioRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class UsuarioAtivoFilter extends OncePerRequestFilter {

    /** Versão da senha autenticada, guardada na sessão para comparar com a versão atual. */
    public static final String ATRIBUTO_LOGIN = "loginEm";

    private final UsuarioRepository usuarios;

    public UsuarioAtivoFilter(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();

        if (autenticacao != null
                && autenticacao.isAuthenticated()
                && !(autenticacao instanceof AnonymousAuthenticationToken)) {

            boolean valida = usuarios.findByLogin(autenticacao.getName())
                    .map(u -> u.isAtivo() && !senhaDaSessaoDiferente(u, request))
                    .orElse(false);

            if (!valida) {
                SecurityContextHolder.clearContext();
                HttpSession sessao = request.getSession(false);
                if (sessao != null) {
                    sessao.invalidate();
                }
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"mensagem\":\"Sessão encerrada. Entre novamente.\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean senhaDaSessaoDiferente(Usuario usuario, HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        Object loginEm = sessao == null ? null : sessao.getAttribute(ATRIBUTO_LOGIN);

        Instant versaoAtual = usuario.getSenhaAlteradaEm() == null ? Instant.EPOCH : usuario.getSenhaAlteradaEm();
        return !versaoAtual.equals(loginEm);
    }
}
