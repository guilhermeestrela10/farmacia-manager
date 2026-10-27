package com.farmacia.farmacia.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.farmacia.farmacia.dto.RelatorioEstoqueResponse;
import com.farmacia.farmacia.dto.RelatorioVendasResponse;
import com.farmacia.farmacia.service.RelatorioService;

@RestController
@RequestMapping("/gestao/relatorios")
public class RelatorioController {

    private final RelatorioService service;

    public RelatorioController(RelatorioService service) {
        this.service = service;
    }

    @GetMapping("/vendas")
    public RelatorioVendasResponse vendas(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return service.vendas(de, ate);
    }

    @GetMapping("/estoque")
    public RelatorioEstoqueResponse estoque(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return service.estoque(de, ate);
    }
}