package com.farmacia.farmacia.controller;
import com.farmacia.farmacia.dto.*;
import com.farmacia.farmacia.service.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
@RestController
public class ConfiguracaoController {
    private final ConfiguracaoService service;
    public ConfiguracaoController(ConfiguracaoService service) { this.service=service; }
    @GetMapping("/configuracoes") public ConfiguracaoResponse buscar() { return service.buscar(); }
    @PutMapping("/gestao/configuracoes") public ConfiguracaoResponse atualizar(@Valid @RequestBody ConfiguracaoRequest r,Authentication auth) {
        if (auth.getAuthorities().stream().noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN,"Acesso restrito ao administrador.");
        return service.atualizar(r);
    }
}
