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
public class AtendimentoService {
    private final AtendimentoRepository repository;
    private final ClienteService clientes;
    public AtendimentoService(AtendimentoRepository repository,ClienteService clientes) { this.repository=repository; this.clientes=clientes; }
    @Transactional public AtendimentoResponse criar(AtendimentoRequest r) {
        return toResponse(repository.save(new Atendimento(clientes.buscarAtivo(r.clienteId()),r.profissional(),r.observacoes())));
    }
    @Transactional(readOnly=true) public List<AtendimentoResponse> listar(Long clienteId) { return repository.listar(clienteId).stream().map(this::toResponse).toList(); }
    private AtendimentoResponse toResponse(Atendimento a) { return new AtendimentoResponse(a.getId(),a.getCliente().getId(),a.getCliente().getNome(),a.getProfissional(),a.getObservacoes(),a.getCriadoEm()); }
}
