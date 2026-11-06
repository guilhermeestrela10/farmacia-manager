package com.farmacia.farmacia.controller;
import com.farmacia.farmacia.dto.*;
import com.farmacia.farmacia.service.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
@RestController @RequestMapping("/dispensacoes")
public class DispensacaoController {
    private final DispensacaoService service;
    public DispensacaoController(DispensacaoService service) { this.service=service; }
    @GetMapping public List<DispensacaoResponse> listar() { return service.listar(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public DispensacaoResponse criar(@Valid @RequestBody DispensacaoRequest r,Authentication auth) { return service.registrar(r,auth.getName()); }
}
