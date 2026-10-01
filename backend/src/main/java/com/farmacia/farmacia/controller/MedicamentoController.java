package com.farmacia.farmacia.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.farmacia.farmacia.dto.MedicamentoRequest;
import com.farmacia.farmacia.dto.MedicamentoResponse;
import com.farmacia.farmacia.service.MedicamentoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/medicamentos")
public class MedicamentoController {

    private final MedicamentoService service;

    public MedicamentoController(MedicamentoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedicamentoResponse criar(@Valid @RequestBody MedicamentoRequest request) {
        return service.criar(request);
    }

    @GetMapping
    public List<MedicamentoResponse> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(defaultValue = "false") boolean incluirInativos) {
        return service.listar(nome, incluirInativos);
    }

    @GetMapping("/{id}")
    public MedicamentoResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public MedicamentoResponse atualizar(@PathVariable Long id,
                                         @Valid @RequestBody MedicamentoRequest request) {
        return service.atualizar(id, request);
    }

    @PatchMapping("/{id}/desativar")
    public MedicamentoResponse desativar(@PathVariable Long id) {
        return service.desativar(id);
    }

    @PatchMapping("/{id}/ativar")
    public MedicamentoResponse ativar(@PathVariable Long id) {
        return service.ativar(id);
    }
}