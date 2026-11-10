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
public class ConfiguracaoRepository extends MongoRepositoryBase<Configuracao> {
    public ConfiguracaoRepository(MongoTemplate mongo) { super(mongo, Configuracao.class); }

}
