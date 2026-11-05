package com.farmacia.farmacia.service;

import com.farmacia.farmacia.dto.AuditoriaResponse;
import com.farmacia.farmacia.model.RegistroAuditoria;
import com.farmacia.farmacia.model.TipoAuditoria;
import com.farmacia.farmacia.repository.AuditoriaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
        Specification<RegistroAuditoria> filtro = (root, query, cb) -> {
            var condicoes = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (de != null) condicoes.add(cb.greaterThanOrEqualTo(root.<Instant>get("dataHora"), de.atStartOfDay(zona).toInstant()));
            if (ate != null) condicoes.add(cb.lessThan(root.<Instant>get("dataHora"), ate.plusDays(1).atStartOfDay(zona).toInstant()));
            if (usuario != null && !usuario.isBlank()) condicoes.add(cb.equal(cb.lower(root.get("usuario")), usuario.trim().toLowerCase(Locale.ROOT)));
            if (tipo != null) condicoes.add(cb.equal(root.get("tipo"), tipo));
            if (somenteRecusadas) condicoes.add(cb.greaterThanOrEqualTo(root.get("status"), 400));
            return cb.and(condicoes.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return repository.findAll(filtro, PageRequest.of(0, limite, Sort.by(Sort.Direction.DESC, "dataHora", "id")))
                .stream().map(r -> new AuditoriaResponse(r.getId(), r.getDataHora(), r.getTipo(),
                        r.getUsuario(), r.getAcao(), r.getMetodo(), r.getCaminho(), r.getStatus(), r.getIp())).toList();
    }
}
