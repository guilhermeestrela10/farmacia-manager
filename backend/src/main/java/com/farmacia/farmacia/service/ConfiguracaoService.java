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
public class ConfiguracaoService {
    private final ConfiguracaoRepository repository;
    public ConfiguracaoService(ConfiguracaoRepository repository) { this.repository=repository; }
    @Transactional(readOnly=true) public ConfiguracaoResponse buscar() { return toResponse(singleton()); }
    @Transactional public ConfiguracaoResponse atualizar(ConfiguracaoRequest r) {
        var c=singleton(); c.atualizar(r.nomeFarmacia(),r.telefone(),r.email(),r.endereco()); return toResponse(repository.save(c));
    }
    private Configuracao singleton() { return repository.findById(1L).orElseThrow(() -> new IllegalStateException("Configuração da farmácia ausente.")); }
    private ConfiguracaoResponse toResponse(Configuracao c) { return new ConfiguracaoResponse(c.getNomeFarmacia(),c.getTelefone(),c.getEmail(),c.getEndereco()); }
}
