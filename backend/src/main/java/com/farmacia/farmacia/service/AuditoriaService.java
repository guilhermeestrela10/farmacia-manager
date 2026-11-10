package com.farmacia.farmacia.service;

import com.farmacia.farmacia.dto.AuditoriaResponse;
import com.farmacia.farmacia.model.RegistroAuditoria;
import com.farmacia.farmacia.model.TipoAuditoria;
import com.farmacia.farmacia.repository.AuditoriaRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuditoriaService {
    private final AuditoriaRepository repository;

    public AuditoriaService(AuditoriaRepository repository) { this.repository = repository; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(TipoAuditoria tipo, String usuario, String acao,
                          String metodo, String caminho, int status, String ip) {
        repository.save(new RegistroAuditoria(tipo, usuario, acao, metodo, caminho, status, ip));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarAlteracao(String usuario, String acao, String metodo, String caminho, int status, String ip) {
        repository.save(new RegistroAuditoria(TipoAuditoria.ALTERACAO, usuario, acao, metodo, caminho, status, ip));
    }

    // A alteração de preço e seu registro devem confirmar ou reverter juntos.
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarPreco(String usuario, String acao, String caminho, String ip) {
        repository.save(new RegistroAuditoria(TipoAuditoria.PRECO, usuario, acao, "PUT", caminho, 200, ip));
    }

    @Transactional(readOnly = true)
    public List<AuditoriaResponse> listar(LocalDate de, LocalDate ate, String usuario,
            TipoAuditoria tipo, boolean somenteRecusadas, int limite) {
        if (limite < 1 || limite > 1000 || (de != null && ate != null && de.isAfter(ate))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Período ou limite inválido (1 a 1000).");
        }
        ZoneId zona = ZoneId.systemDefault();
        Query filtro = new Query();
        if (de != null || ate != null) {
            Criteria periodo = Criteria.where("dataHora");
            if (de != null) periodo.gte(de.atStartOfDay(zona).toInstant());
            if (ate != null) periodo.lt(ate.plusDays(1).atStartOfDay(zona).toInstant());
            filtro.addCriteria(periodo);
        }
        if (usuario != null && !usuario.isBlank()) {
            filtro.addCriteria(Criteria.where("usuario").regex(Pattern.compile(
                    "^" + Pattern.quote(usuario.trim()) + "$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)));
        }
        if (tipo != null) filtro.addCriteria(Criteria.where("tipo").is(tipo));
        if (somenteRecusadas) filtro.addCriteria(Criteria.where("status").gte(400));
        return repository.findAll(filtro.with(Sort.by(Sort.Direction.DESC, "dataHora", "id")).limit(limite))
                .stream().map(r -> new AuditoriaResponse(r.getId(), r.getDataHora(), r.getTipo(),
                        r.getUsuario(), r.getAcao(), r.getMetodo(), r.getCaminho(), r.getStatus(), r.getIp())).toList();
    }
}
