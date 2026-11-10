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
public class LoteRepository extends MongoRepositoryBase<Lote> {
    public LoteRepository(MongoTemplate mongo) { super(mongo, Lote.class); }
    public Optional<Lote> findByMedicamentoIdAndNumeroLote(Long medicamentoId, String numeroLote) {
        return locked(by("medicamento.$id", medicamentoId).addCriteria(Criteria.where("numeroLote").is(numeroLote)));
    }
    public List<Lote> findByMedicamentoIdAndQuantidadeAtualGreaterThanOrderByValidade(Long medicamentoId, int quantidade) {
        return findAll(by("medicamento.$id", medicamentoId).addCriteria(Criteria.where("quantidadeAtual").gt(quantidade)).with(Sort.by("validade", "id")));
    }
    public List<Object[]> resumirSaldoPorMedicamento(Long medicamentoId, LocalDate hoje) {
        var lotes = findAll(by("medicamento.$id", medicamentoId));
        long total = lotes.stream().mapToLong(Lote::getQuantidadeAtual).sum();
        long disponivel = lotes.stream().filter(l -> !l.getValidade().isBefore(hoje)).mapToLong(Lote::getQuantidadeAtual).sum();
        return Collections.singletonList(new Object[]{total, disponivel});
    }
    public List<Lote> buscarDisponiveisParaSaida(Long medicamentoId, LocalDate hoje) {
        return lockedAll(by("medicamento.$id", medicamentoId).addCriteria(Criteria.where("quantidadeAtual").gt(0)).addCriteria(Criteria.where("validade").gte(hoje)).with(Sort.by("validade", "id")));
    }
    public Optional<Lote> buscarPorIdComTravamento(Long id) { return locked(by("id", id)); }
    public List<Lote> buscarVencendoEntre(LocalDate inicio, LocalDate fim) {
        return findAll(comSaldoEntre(inicio, fim).with(Sort.by("validade", "id")));
    }
    public List<Lote> buscarVencidosComSaldo(LocalDate hoje) {
        return findAll(Query.query(Criteria.where("quantidadeAtual").gt(0)).addCriteria(Criteria.where("validade").lt(hoje)).with(Sort.by("validade", "id")));
    }
    public List<Object[]> resumoPorValidade(LocalDate de, LocalDate ate) {
        var lotes = findAll(comSaldoEntre(de, ate));
        if (lotes.isEmpty()) return Collections.singletonList(new Object[]{null, null});
        long quantidade = lotes.stream().mapToLong(Lote::getQuantidadeAtual).sum();
        BigDecimal valor = lotes.stream().map(l -> l.getMedicamento().getPreco().multiply(BigDecimal.valueOf(l.getQuantidadeAtual()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        return Collections.singletonList(new Object[]{quantidade, valor});
    }
    public List<Lote> buscarComSaldo(String busca, LocalDate inicio, LocalDate fim) {
        Query query = comSaldoEntre(inicio, fim);
        var ids = mongo.find(by("nome", contains(busca)), Medicamento.class).stream().map(Medicamento::getId).toList();
        query.addCriteria(new Criteria().orOperator(Criteria.where("numeroLote").regex(contains(busca)), Criteria.where("medicamento.$id").in(ids)));
        return findAll(query.with(Sort.by("validade", "id")));
    }
    private static Query comSaldoEntre(LocalDate inicio, LocalDate fim) {
        return Query.query(Criteria.where("quantidadeAtual").gt(0).and("validade").gte(inicio).lte(fim));
    }
}
