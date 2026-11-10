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
public class ReceitaRepository extends MongoRepositoryBase<Receita> {
    public ReceitaRepository(MongoTemplate mongo) { super(mongo, Receita.class); }
    public Optional<Receita> buscarPorIdComTravamento(Long id) { return locked(by("id", id)); }
    public List<Receita> listar(Long clienteId) { return findAll((clienteId == null ? new Query() : by("cliente.$id", clienteId)).with(Sort.by(Sort.Direction.DESC, "id"))); }
}
