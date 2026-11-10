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
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class AuditoriaFilter extends OncePerRequestFilter {
    private final AuditoriaService service;
    private final TransactionTemplate transacao;

    public AuditoriaFilter(AuditoriaService service, PlatformTransactionManager transacoes) {
        this.service = service;
        this.transacao = new TransactionTemplate(transacoes);
    }

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
        String caminho = req.getRequestURI();
        String acao = DescritorDeAcao.descrever(metodo, caminho);
        var resposta = new ContentCachingResponseWrapper(res);
        try {
            // A alteração e sua auditoria confirmam juntas, antes de liberar a resposta.
            transacao.executeWithoutResult(status -> {
                try {
                    chain.doFilter(req, resposta);
                } catch (ServletException | IOException ex) {
                    throw new FalhaNaRequisicao(ex);
                }
                if (resposta.getStatus() >= 400) {
                    status.setRollbackOnly();
                } else {
                    service.registrarAlteracao(usuario, acao, metodo, caminho, resposta.getStatus(), req.getRemoteAddr());
                }
            });
        } catch (RuntimeException ex) {
            boolean conflito = com.farmacia.farmacia.exception.ConflitosMongo.concorrente(ex);
            try {
                service.registrar(TipoAuditoria.ALTERACAO, usuario, acao, metodo, caminho, conflito ? 409 : 500, req.getRemoteAddr());
            } catch (RuntimeException falhaAuditoria) {
                ex.addSuppressed(falhaAuditoria);
            }
            if (conflito) {
                res.setStatus(409);
                res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"mensagem\":\"Outra operação alterou estes dados. Atualize a página e confira o resultado antes de tentar novamente.\"}");
                return;
            }
            if (ex instanceof FalhaNaRequisicao && ex.getCause() instanceof ServletException servlet) throw servlet;
            if (ex instanceof FalhaNaRequisicao && ex.getCause() instanceof IOException io) throw io;
            throw ex;
        }
        // Recusas ficam registradas fora da transação revertida, sem corpo ou query string.
        if (resposta.getStatus() >= 400) {
            service.registrar(TipoAuditoria.ALTERACAO, usuario, acao, metodo, caminho, resposta.getStatus(), req.getRemoteAddr());
        }
        resposta.copyBodyToResponse();
    }

    private static class FalhaNaRequisicao extends RuntimeException {
        FalhaNaRequisicao(Exception causa) { super(causa); }
    }
}
