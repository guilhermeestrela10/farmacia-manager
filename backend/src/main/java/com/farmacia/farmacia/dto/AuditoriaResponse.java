package com.farmacia.farmacia.dto;

import com.farmacia.farmacia.model.TipoAuditoria;
import java.time.Instant;

public record AuditoriaResponse(Long id, Instant dataHora, TipoAuditoria tipo,
        String usuario, String acao, String metodo, String caminho, int status, String ip) {}
