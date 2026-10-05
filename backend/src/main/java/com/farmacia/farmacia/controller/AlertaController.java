package com.farmacia.farmacia.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.farmacia.farmacia.dto.AlertaEstoqueResponse;
import com.farmacia.farmacia.dto.AlertaLoteResponse;
import com.farmacia.farmacia.dto.ResumoAlertasResponse;
import com.farmacia.farmacia.service.AlertaService;

@RestController
@RequestMapping("/alertas")
public class AlertaController {

    private final AlertaService service;

    public AlertaController(AlertaService service) {
        this.service = service;
    }

    @GetMapping("/resumo")
    public ResumoAlertasResponse resumo(@RequestParam(defaultValue = "90") int dias) {
        return service.resumo(dias);
    }

    @GetMapping("/sem-estoque")
    public List<AlertaEstoqueResponse> semEstoque() {
        return service.listarSemEstoque();
    }

    @GetMapping("/estoque-baixo")
    public List<AlertaEstoqueResponse> estoqueBaixo() {
        return service.listarEstoqueBaixo();
    }

    @GetMapping("/vencimento-proximo")
    public List<AlertaLoteResponse> vencimentoProximo(
            @RequestParam(defaultValue = "90") int dias) {
        return service.listarVencimentoProximo(dias);
    }

    @GetMapping("/vencidos")
    public List<AlertaLoteResponse> vencidos() {
        return service.listarVencidos();
    }
}