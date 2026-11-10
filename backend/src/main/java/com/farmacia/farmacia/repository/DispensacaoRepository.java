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
public class DispensacaoRepository extends MongoRepositoryBase<Dispensacao> {
    public DispensacaoRepository(MongoTemplate mongo) { super(mongo, Dispensacao.class); }
    public long quantidadeDispensada(Long receitaId, Long medicamentoId) {
        return findAll(by("receita.$id", receitaId)).stream().map(Dispensacao::getVenda)
            .filter(v -> v.getStatus() == StatusVenda.CONCLUIDA).flatMap(v -> v.getItens().stream())
            .filter(i -> i.getMedicamento().getId().equals(medicamentoId)).mapToLong(ItemVenda::getQuantidade).sum();
    }
    public List<Dispensacao> findAllByOrderByCriadaEmDescIdDesc() { return findAll(Sort.by(Sort.Direction.DESC, "criadaEm", "id")); }
}
