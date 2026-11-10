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
public class FabricanteRepository extends MongoRepositoryBase<Fabricante> {
    public FabricanteRepository(MongoTemplate mongo) { super(mongo, Fabricante.class); }
    public boolean existsByNomeIgnoreCase(String nome) { return mongo.exists(by("nome", exact(nome)), type); }
}
