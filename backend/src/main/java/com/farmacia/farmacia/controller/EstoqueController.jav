package com.farmacia.farmacia.controller;

import com.farmacia.farmacia.dto.EntradaEstoqueRequest;
import com.farmacia.farmacia.dto.LoteResponse;
import com.farmacia.farmacia.dto.MovimentacaoResponse;
import com.farmacia.farmacia.dto.SaldoResponse;
import com.farmacia.farmacia.service.EstoqueService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/estoque")
public class EstoqueController {

    private final EstoqueService service;

    public EstoqueController(EstoqueService service) {
        this.service = service;
    }

    @PostMapping("/entradas")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimentacaoResponse registrarEntrada(
            @Valid @RequestBody EntradaEstoqueRequest request) {
        return service.registrarEntrada(request);
    }

    @GetMapping("/medicamentos/{medicamentoId}/lotes")
    public List<LoteResponse> listarLotes(@PathVariable Long medicamentoId) {
        return service.listarLotes(medicamentoId);
    }

    @GetMapping("/medicamentos/{medicamentoId}/saldo")
    public SaldoResponse consultarSaldo(@PathVariable Long medicamentoId) {
        return service.consultarSaldo(medicamentoId);
    }
}