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
public class ClienteRepository extends MongoRepositoryBase<Cliente> {
    public ClienteRepository(MongoTemplate mongo) { super(mongo, Cliente.class); }
    public Optional<Cliente> buscarPorIdComTravamento(Long id) { return locked(by("id", id)); }
    public List<Cliente> buscar(String busca) { return findAll(by("nome", contains(busca)).with(Sort.by("nome", "id"))); }
}
