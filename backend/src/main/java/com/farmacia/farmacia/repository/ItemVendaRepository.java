package com.farmacia.farmacia.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.farmacia.farmacia.model.ItemVenda;
import com.farmacia.farmacia.model.StatusVenda;

public interface ItemVendaRepository extends JpaRepository<ItemVenda, Long> {

    @Query("select i.medicamento.id, i.medicamento.nome, "
            + "sum(i.quantidade), sum(i.quantidade * i.precoUnitario) "
            + "from ItemVenda i "
            + "where i.venda.status = :status "
            + "and i.venda.criadaEm >= :inicio and i.venda.criadaEm < :fim "
            + "group by i.medicamento.id, i.medicamento.nome "
            + "order by sum(i.quantidade) desc, i.medicamento.nome")
    List<Object[]> maisVendidos(@Param("status") StatusVenda status,
                                @Param("inicio") Instant inicio,
                                @Param("fim") Instant fim,
                                Pageable limite);

    @Query("select i.medicamento.categoria.nome, "
            + "sum(i.quantidade), sum(i.quantidade * i.precoUnitario) "
            + "from ItemVenda i "
            + "where i.venda.status = :status "
            + "and i.venda.criadaEm >= :inicio and i.venda.criadaEm < :fim "
            + "group by i.medicamento.categoria.nome "
            + "order by sum(i.quantidade * i.precoUnitario) desc")
    List<Object[]> totaisPorCategoria(@Param("status") StatusVenda status,
                                      @Param("inicio") Instant inicio,
                                      @Param("fim") Instant fim);
}