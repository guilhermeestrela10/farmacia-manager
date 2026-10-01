package com.farmacia.farmacia.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.farmacia.farmacia.dto.FabricanteRequest;
import com.farmacia.farmacia.dto.FabricanteResponse;
import com.farmacia.farmacia.service.FabricanteService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/fabricantes")
public class FabricanteController {

    private final FabricanteService service;

    public FabricanteController(FabricanteService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FabricanteResponse criar(@Valid @RequestBody FabricanteRequest request) {
        return service.criar(request);
    }

    @GetMapping
    public List<FabricanteResponse> listar() {
        return service.listar();
    }
}