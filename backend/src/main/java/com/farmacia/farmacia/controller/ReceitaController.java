package com.farmacia.farmacia.controller;
import com.farmacia.farmacia.dto.*;
import com.farmacia.farmacia.service.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
@RestController @RequestMapping("/receitas")
public class ReceitaController {
    private final ReceitaService service;
    public ReceitaController(ReceitaService service) { this.service=service; }
    @GetMapping public List<ReceitaResponse> listar(@RequestParam(required=false) Long clienteId) { return service.listar(clienteId); }
    @GetMapping("/{id}") public ReceitaResponse buscar(@PathVariable Long id) { return service.buscar(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ReceitaResponse criar(@Valid @RequestBody ReceitaRequest r) { return service.criar(r); }
}
