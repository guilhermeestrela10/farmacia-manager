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
public class MedicamentoRepository extends MongoRepositoryBase<Medicamento> {
    public MedicamentoRepository(MongoTemplate mongo) { super(mongo, Medicamento.class); }
    public Optional<Medicamento> buscarPorIdComTravamento(Long id) { return locked(by("id", id)); }
    public boolean existsByCodigoBarras(String codigoBarras) { return mongo.exists(by("codigoBarras", codigoBarras), type); }
    public boolean existsByCodigoBarrasAndIdNot(String codigoBarras, Long id) { return mongo.exists(by("codigoBarras", codigoBarras).addCriteria(Criteria.where("id").ne(id)), type); }
    public List<Medicamento> listarTodosComRelacionamentos() { return findAll(Sort.by("nome")); }
    public List<Object[]> saldoDisponivelDosAtivos(LocalDate hoje) {
        var ativos = findAll(by("ativo", true).with(Sort.by("nome")));
        var ids = ativos.stream().map(Medicamento::getId).toList();
        var lotes = mongo.find(Query.query(Criteria.where("medicamento.$id").in(ids).and("validade").gte(hoje)), Lote.class);
        Map<Long, Long> saldos = new HashMap<>();
        lotes.forEach(l -> saldos.merge(l.getMedicamento().getId(), (long) l.getQuantidadeAtual(), Long::sum));
        return ativos.stream().map(m -> new Object[]{m.getId(), m.getNome(), m.getEstoqueMinimo(), saldos.getOrDefault(m.getId(), 0L)}).toList();
    }
}
