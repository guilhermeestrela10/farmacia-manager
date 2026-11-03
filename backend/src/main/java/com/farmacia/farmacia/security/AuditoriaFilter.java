package com.farmacia.farmacia.security;

import com.farmacia.farmacia.model.TipoAuditoria;
import com.farmacia.farmacia.service.AuditoriaService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Set;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class AuditoriaFilter extends OncePerRequestFilter {
    private final AuditoriaService service;
    public AuditoriaFilter(AuditoriaService service) { this.service = service; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String metodo = req.getMethod();
        if (Set.of("GET", "HEAD", "OPTIONS").contains(metodo)
                || Set.of("/login", "/logout").contains(req.getServletPath().isEmpty() ? req.getRequestURI() : req.getServletPath())) {
            chain.doFilter(req, res);
            return;
        }
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = auth == null || auth instanceof AnonymousAuthenticationToken ? "anônimo" : auth.getName();
        boolean concluida = false;
        try {
            chain.doFilter(req, res);
            concluida = true;
        } finally {
            // Nunca ler corpo, parâmetros ou query string: podem conter senhas.
            String caminho = req.getRequestURI();
            service.registrar(TipoAuditoria.ALTERACAO, usuario, DescritorDeAcao.descrever(metodo, caminho),
                    metodo, caminho, concluida ? res.getStatus() : 500, req.getRemoteAddr());
        }
    }
}
