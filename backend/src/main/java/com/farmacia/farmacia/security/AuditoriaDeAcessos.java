package com.farmacia.farmacia.security;

import com.farmacia.farmacia.model.TipoAuditoria;
import com.farmacia.farmacia.service.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class AuditoriaDeAcessos {
    private final AuditoriaService service;
    public AuditoriaDeAcessos(AuditoriaService service) { this.service = service; }

    public void registrar(HttpServletRequest req, String login, String acao, int status) {
        service.registrar(TipoAuditoria.ACESSO,
                login == null ? "anônimo" : login.trim().toLowerCase(Locale.ROOT), acao,
                req.getMethod(), req.getRequestURI(), status, req.getRemoteAddr());
    }
}
