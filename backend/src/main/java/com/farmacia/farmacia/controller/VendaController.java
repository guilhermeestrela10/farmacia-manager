package com.farmacia.farmacia.controller;

import com.farmacia.farmacia.dto.ProdutoVendaResponse;
import com.farmacia.farmacia.dto.VendaRequest;
import com.farmacia.farmacia.dto.VendaResponse;
import com.farmacia.farmacia.model.TipoMedicamento;
import com.farmacia.farmacia.service.VendaService;
import jakarta.validation.Valid;
import java.util.List;
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

    @GetMapping("/{id}")
    public VendaResponse buscar(@PathVariable Long id, Authentication autenticacao) {
        boolean administrador = autenticacao.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return service.buscar(id, autenticacao.getName(), administrador);
    }
}