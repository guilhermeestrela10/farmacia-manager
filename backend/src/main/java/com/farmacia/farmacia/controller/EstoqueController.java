package com.farmacia.farmacia.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.farmacia.farmacia.dto.AjusteEstoqueRequest;
import com.farmacia.farmacia.dto.EntradaEstoqueRequest;
import com.farmacia.farmacia.dto.LoteResponse;
import com.farmacia.farmacia.dto.MovimentacaoResponse;
import com.farmacia.farmacia.dto.PerdaEstoqueRequest;
import com.farmacia.farmacia.dto.SaidaEstoqueRequest;
import com.farmacia.farmacia.dto.SaldoResponse;
import com.farmacia.farmacia.model.TipoMovimentacao;
import com.farmacia.farmacia.service.EstoqueService;

import jakarta.validation.Valid;

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

    @PostMapping("/saidas")
    @ResponseStatus(HttpStatus.CREATED)
    public List<MovimentacaoResponse> registrarSaida(
            @Valid @RequestBody SaidaEstoqueRequest request) {
        return service.registrarSaida(request);
    }

    @PostMapping("/ajustes")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimentacaoResponse registrarAjuste(
            @Valid @RequestBody AjusteEstoqueRequest request) {
        return service.registrarAjuste(request);
    }

    @PostMapping("/perdas")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimentacaoResponse registrarPerda(
            @Valid @RequestBody PerdaEstoqueRequest request) {
        return service.registrarPerda(request);
    }

    @GetMapping("/movimentacoes")
    public List<MovimentacaoResponse> listarMovimentacoes(
            @RequestParam(required = false) Long medicamentoId,
            @RequestParam(required = false) TipoMovimentacao tipo,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(defaultValue = "100") int limite) {
        return service.listarMovimentacoes(medicamentoId, tipo, de, ate, limite);
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