package com.farmacia.farmacia.repository;
import com.farmacia.farmacia.model.*;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
@Repository
public class AuditoriaRepository extends MongoRepositoryBase<RegistroAuditoria> {
    public AuditoriaRepository(MongoTemplate mongo) { super(mongo, RegistroAuditoria.class); }
    @Override public <S extends RegistroAuditoria> S save(S entity) {
        if (entity.getId() != null && existsById(entity.getId())) throw imutavel();
        return mongo.insert(entity);
    }
    @Override public void deleteAll() { throw imutavel(); }
    @Override public void deleteAll(Iterable<? extends RegistroAuditoria> entities) { throw imutavel(); }
    @Override public void deleteById(Long id) { throw imutavel(); }
    @Override public void delete(RegistroAuditoria entity) { throw imutavel(); }
    private org.springframework.dao.DataIntegrityViolationException imutavel() {
        return new org.springframework.dao.DataIntegrityViolationException("Registros de auditoria são imutáveis.");
    }

}
