package com.farmacia.farmacia.controller;

import com.farmacia.farmacia.dto.AuditoriaResponse;
import com.farmacia.farmacia.model.TipoAuditoria;
import com.farmacia.farmacia.service.AuditoriaService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/gestao/auditoria")
public class AuditoriaController {
    private final AuditoriaService service;
    public AuditoriaController(AuditoriaService service) { this.service = service; }

    @GetMapping
    public List<AuditoriaResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) TipoAuditoria tipo,
            @RequestParam(defaultValue = "false") boolean somenteRecusadas,
            @RequestParam(defaultValue = "100") int limite) {
        return service.listar(de, ate, usuario, tipo, somenteRecusadas, limite);
    }
}
