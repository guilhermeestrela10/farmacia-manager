package com.farmacia.farmacia.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.farmacia.farmacia.model.MovimentacaoEstoque;
import com.farmacia.farmacia.model.TipoMovimentacao;

public interface MovimentacaoEstoqueRepository
        extends JpaRepository<MovimentacaoEstoque, Long>,
        JpaSpecificationExecutor<MovimentacaoEstoque> {

    @Query("select m from MovimentacaoEstoque m where m.vendaId = :vendaId and m.tipo = :tipo "
            + "order by m.lote.validade, m.lote.id, m.id")
    List<MovimentacaoEstoque> findByVendaIdAndTipo(@Param("vendaId") Long vendaId, @Param("tipo") TipoMovimentacao tipo);

    @Query("select sum(m.quantidade), sum(m.quantidade * m.lote.medicamento.preco) "
            + "from MovimentacaoEstoque m "
            + "where m.tipo = :tipo and m.dataHora >= :inicio and m.dataHora < :fim")
    List<Object[]> somarPorTipo(@Param("tipo") TipoMovimentacao tipo,
                                @Param("inicio") Instant inicio,
                                @Param("fim") Instant fim);
}
