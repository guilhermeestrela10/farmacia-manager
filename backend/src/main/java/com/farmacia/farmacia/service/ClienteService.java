package com.farmacia.farmacia.service;
import com.farmacia.farmacia.dto.*;
import com.farmacia.farmacia.model.*;
import com.farmacia.farmacia.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
import java.time.LocalDate;
@Service
public class ClienteService {
    private final ClienteRepository repository;
    public ClienteService(ClienteRepository repository) { this.repository=repository; }
    @Transactional public ClienteResponse criar(ClienteRequest request) {
        return toResponse(repository.save(new Cliente(request.nome(),request.telefone(),request.email())));
    }
    @Transactional(readOnly=true) public List<ClienteResponse> listar(String busca) {
        return repository.buscar(busca==null ? "" : busca.trim()).stream().map(this::toResponse).toList();
    }
    @Transactional public ClienteResponse definirAtivo(Long id,boolean ativo) {
        var cliente=repository.buscarPorIdComTravamento(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Cliente não encontrado."));
        cliente.definirAtivo(ativo);
        return toResponse(cliente);
    }
    @Transactional public Cliente buscarAtivo(Long id) {
        var cliente=repository.buscarPorIdComTravamento(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Cliente não encontrado."));
        if (!cliente.isAtivo()) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Cliente desativado.");
        return cliente;
    }
    private ClienteResponse toResponse(Cliente c) { return new ClienteResponse(c.getId(),c.getNome(),c.getTelefone(),c.getEmail(),c.isAtivo()); }
}
