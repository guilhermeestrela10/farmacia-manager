package com.farmacia.farmacia.repository;

import java.util.*;
import java.util.regex.Pattern;
import org.bson.Document;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public abstract class MongoRepositoryBase<T> {
    protected final MongoTemplate mongo;
    protected final Class<T> type;
    protected MongoRepositoryBase(MongoTemplate mongo, Class<T> type) { this.mongo = mongo; this.type = type; }
    public <S extends T> S save(S entity) { return mongo.save(entity); }
    public <S extends T> S saveAndFlush(S entity) { return save(entity); }
    public <S extends T> List<S> saveAll(Iterable<S> entities) {
        List<S> result = new ArrayList<>(); entities.forEach(entity -> result.add(save(entity))); return result;
    }
    public void flush() { /* MongoDB writes are immediate within the current transaction. */ }
    public Optional<T> findById(Long id) { return Optional.ofNullable(mongo.findById(id, type)); }
    public T getReferenceById(Long id) { return findById(id).orElseThrow(() -> new NoSuchElementException("Documento ausente: " + id)); }
    public boolean existsById(Long id) { return mongo.exists(Query.query(Criteria.where("id").is(id)), type); }
    public List<T> findAll() { return findAll(new Query()); }
    public List<T> findAll(Sort sort) { return findAll(new Query().with(sort)); }
    public List<T> findAll(Query query) { return mongo.find(query, type); }
    public Page<T> findAll(Query query, Pageable page) {
        long total = count(Query.of(query).skip(-1).limit(-1));
        return new PageImpl<>(findAll(Query.of(query).with(page)), page, total);
    }
    public List<T> findAllById(Iterable<Long> ids) { List<Long> values = new ArrayList<>(); ids.forEach(values::add); return findAll(Query.query(Criteria.where("id").in(values))); }
    public long count() { return count(new Query()); }
    public long count(Query query) { return mongo.count(query, type); }
    public void deleteAll() { mongo.remove(new Query(), type); }
    public void deleteAll(Iterable<? extends T> entities) { entities.forEach(mongo::remove); }
    public void deleteById(Long id) { mongo.remove(Query.query(Criteria.where("id").is(id)), type); }
    public void delete(T entity) { mongo.remove(entity); }
    protected Optional<T> locked(Query query) {
        requireTransaction();
        // Explicit zero increment prevents Spring from automatically incrementing @Version here.
        return Optional.ofNullable(mongo.findAndModify(query, new Update().inc("_guardVersion", 1L).inc("version", 0L), FindAndModifyOptions.options().returnNew(true), type));
    }
    protected List<T> lockedAll(Query query) {
        List<T> result = new ArrayList<>();
        for (T entity : findAll(query)) {
            Object id = mongo.getConverter().getMappingContext().getRequiredPersistentEntity(type).getIdentifierAccessor(entity).getIdentifier();
            locked(Query.query(Criteria.where("id").is(id))).ifPresent(result::add);
        }
        return result;
    }
    protected void adminGuard() {
        requireTransaction();
        if (mongo.findAndModify(Query.query(Criteria.where("_id").is("administradores")), new Update().inc("version", 1L), Document.class, "_guards") == null) throw new IllegalStateException("Guarda de administradores ausente.");
    }
    private static void requireTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("Travamento exige transação MongoDB.");
    }
    protected static Query by(String field, Object value) { return Query.query(Criteria.where(field).is(value)); }
    protected static Pattern contains(String value) { return Pattern.compile(Pattern.quote(value), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE); }
    protected static Pattern exact(String value) { return Pattern.compile("^" + Pattern.quote(value) + "$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE); }
}
