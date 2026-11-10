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
public class ItemVendaRepository {
    private final MongoTemplate mongo;
    public ItemVendaRepository(MongoTemplate mongo) { this.mongo = mongo; }
    private List<ItemVenda> itens(StatusVenda status, Instant inicio, Instant fim) {
        return mongo.find(Query.query(Criteria.where("status").is(status).and("criadaEm").gte(inicio).lt(fim)), Venda.class).stream().flatMap(v -> v.getItens().stream()).toList();
    }
    public List<Object[]> maisVendidos(StatusVenda status, Instant inicio, Instant fim, Pageable limite) {
        Map<Long, Object[]> totais = new HashMap<>();
        for (var item : itens(status, inicio, fim)) {
            var medicamento = item.getMedicamento();
            var row = totais.computeIfAbsent(medicamento.getId(), id -> new Object[]{id, medicamento.getNome(), 0L, BigDecimal.ZERO});
            row[2] = (Long) row[2] + item.getQuantidade(); row[3] = ((BigDecimal) row[3]).add(item.getSubtotal());
        }
        var stream = totais.values().stream().sorted(Comparator.<Object[], Long>comparing(r -> (Long) r[2]).reversed().thenComparing(r -> (String) r[1]));
        return limite.isPaged() ? stream.skip(limite.getOffset()).limit(limite.getPageSize()).toList() : stream.toList();
    }
    public List<Object[]> totaisPorCategoria(StatusVenda status, Instant inicio, Instant fim) {
        Map<String, Object[]> totais = new HashMap<>();
        for (var item : itens(status, inicio, fim)) {
            String categoria = item.getMedicamento().getCategoria().getNome();
            var row = totais.computeIfAbsent(categoria, nome -> new Object[]{nome, 0L, BigDecimal.ZERO});
            row[1] = (Long) row[1] + item.getQuantidade(); row[2] = ((BigDecimal) row[2]).add(item.getSubtotal());
        }
        return totais.values().stream().sorted(Comparator.<Object[], BigDecimal>comparing(r -> (BigDecimal) r[2]).reversed()).toList();
    }
}
