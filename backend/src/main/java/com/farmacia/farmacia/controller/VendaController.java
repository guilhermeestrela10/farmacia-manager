package com.farmacia.farmacia.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.farmacia.farmacia.dto.CancelamentoRequest;
import com.farmacia.farmacia.dto.HistoricoVendasResponse;
import com.farmacia.farmacia.dto.ProdutoVendaResponse;
import com.farmacia.farmacia.dto.VendaRequest;
import com.farmacia.farmacia.dto.VendaResponse;
import com.farmacia.farmacia.model.StatusVenda;
import com.farmacia.farmacia.model.TipoMedicamento;
import com.farmacia.farmacia.service.VendaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/vendas")
public class VendaController {

    private final VendaService service;

    public VendaController(VendaService service) {
        this.service = service;
    }

    @GetMapping("/produtos")
    public List<ProdutoVendaResponse> produtos(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) TipoMedicamento tipo) {
        return service.listarProdutos(busca, categoriaId, tipo);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VendaResponse registrar(@Valid @RequestBody VendaRequest request,
                                   Authentication autenticacao) {
        return service.registrar(request, autenticacao.getName());
    }

    @GetMapping
    public HistoricoVendasResponse listar(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) Long vendedorId,
            @RequestParam(required = false) StatusVenda status,
            Authentication autenticacao) {
        return service.listar(de, ate, vendedorId, status,
                autenticacao.getName(), ehAdministrador(autenticacao));
    }

    @GetMapping("/{id}")
    public VendaResponse buscar(@PathVariable Long id, Authentication autenticacao) {
        return service.buscar(id, autenticacao.getName(), ehAdministrador(autenticacao));
    }

    @PostMapping("/{id}/cancelar")
    public VendaResponse cancelar(@PathVariable Long id,
                                  @Valid @RequestBody CancelamentoRequest request,
                                  Authentication autenticacao) {
        return service.cancelar(id, request.motivo(),
                autenticacao.getName(), ehAdministrador(autenticacao));
    }

    private boolean ehAdministrador(Authentication autenticacao) {
        return autenticacao.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}