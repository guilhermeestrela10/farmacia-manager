package com.farmacia.farmacia.controller;
import com.farmacia.farmacia.dto.*;
import com.farmacia.farmacia.service.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
@RestController @RequestMapping("/atendimentos")
public class AtendimentoController {
    private final AtendimentoService service;
    public AtendimentoController(AtendimentoService service) { this.service=service; }
    @GetMapping public List<AtendimentoResponse> listar(@RequestParam(required=false) Long clienteId) { return service.listar(clienteId); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public AtendimentoResponse criar(@Valid @RequestBody AtendimentoRequest r) { return service.criar(r); }
}
