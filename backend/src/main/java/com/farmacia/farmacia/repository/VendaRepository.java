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
public class VendaRepository extends MongoRepositoryBase<Venda> {
    public VendaRepository(MongoTemplate mongo) { super(mongo, Venda.class); }
    public Optional<Venda> buscarPorIdComTravamento(Long id) { return locked(by("id", id)); }
}
