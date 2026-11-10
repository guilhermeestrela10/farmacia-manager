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
public class MovimentacaoEstoqueRepository extends MongoRepositoryBase<MovimentacaoEstoque> {
    public MovimentacaoEstoqueRepository(MongoTemplate mongo) { super(mongo, MovimentacaoEstoque.class); }
    public List<MovimentacaoEstoque> findByVendaIdAndTipo(Long vendaId, TipoMovimentacao tipo) {
        return findAll(by("vendaId", vendaId).addCriteria(Criteria.where("tipo").is(tipo))).stream()
            .sorted(Comparator.comparing((MovimentacaoEstoque m) -> m.getLote().getMedicamento().getId())
                .thenComparing(m -> m.getLote().getValidade()).thenComparing(m -> m.getLote().getId()).thenComparing(MovimentacaoEstoque::getId)).toList();
    }
    public List<Object[]> somarPorTipo(TipoMovimentacao tipo, Instant inicio, Instant fim) {
        var movimentos = findAll(by("tipo", tipo).addCriteria(Criteria.where("dataHora").gte(inicio).lt(fim)));
        if (movimentos.isEmpty()) return Collections.singletonList(new Object[]{null, null});
        long quantidade = movimentos.stream().mapToLong(MovimentacaoEstoque::getQuantidade).sum();
        BigDecimal valor = movimentos.stream().map(m -> m.getLote().getMedicamento().getPreco().multiply(BigDecimal.valueOf(m.getQuantidade()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        return Collections.singletonList(new Object[]{quantidade, valor});
    }
}
