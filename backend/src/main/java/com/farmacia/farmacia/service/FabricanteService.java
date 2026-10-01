package com.farmacia.farmacia.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.farmacia.farmacia.dto.FabricanteRequest;
import com.farmacia.farmacia.dto.FabricanteResponse;
import com.farmacia.farmacia.model.Fabricante;
import com.farmacia.farmacia.repository.FabricanteRepository;

@Service
public class FabricanteService {

    private final FabricanteRepository repository;

    public FabricanteService(FabricanteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public FabricanteResponse criar(FabricanteRequest request) {
        String nome = request.nome().trim();

        if (repository.existsByNomeIgnoreCase(nome)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Já existe um fabricante com esse nome.");
        }

        Fabricante salvo = repository.save(new Fabricante(nome));
        return toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public List<FabricanteResponse> listar() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private FabricanteResponse toResponse(Fabricante fabricante) {
        return new FabricanteResponse(fabricante.getId(), fabricante.getNome());
    }
}