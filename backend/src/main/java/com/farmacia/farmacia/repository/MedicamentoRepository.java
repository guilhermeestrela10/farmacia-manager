package com.farmacia.farmacia.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.farmacia.farmacia.model.Medicamento;

public interface MedicamentoRepository extends JpaRepository<Medicamento, Long> {

    boolean existsByCodigoBarras(String codigoBarras);

    boolean existsByCodigoBarrasAndIdNot(String codigoBarras, Long id);

    @Query("select m from Medicamento m "
            + "join fetch m.categoria join fetch m.fabricante "
            + "order by m.nome")
    List<Medicamento> listarTodosComRelacionamentos();

    @Query("select m.id, m.nome, m.estoqueMinimo, "
            + "coalesce(sum(case when l.validade >= :hoje "
            + "then l.quantidadeAtual else 0 end), 0L) "
            + "from Medicamento m left join Lote l on l.medicamento = m "
            + "where m.ativo = true "
            + "group by m.id, m.nome, m.estoqueMinimo "
            + "order by m.nome")
    List<Object[]> saldoDisponivelDosAtivos(@Param("hoje") LocalDate hoje);
}