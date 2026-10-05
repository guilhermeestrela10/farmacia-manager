package com.farmacia.farmacia.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.farmacia.farmacia.model.Lote;

import jakarta.persistence.LockModeType;

public interface LoteRepository extends JpaRepository<Lote, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Lote> findByMedicamentoIdAndNumeroLote(Long medicamentoId, String numeroLote);

    List<Lote> findByMedicamentoIdAndQuantidadeAtualGreaterThanOrderByValidade(
            Long medicamentoId, int quantidade);

    @Query("select coalesce(sum(l.quantidadeAtual), 0L) from Lote l "
            + "where l.medicamento.id = :medicamentoId")
    long somarQuantidadePorMedicamento(@Param("medicamentoId") Long medicamentoId);

    @Query("select coalesce(sum(l.quantidadeAtual), 0L) from Lote l "
            + "where l.medicamento.id = :medicamentoId and l.validade >= :hoje")
    long somarDisponivelPorMedicamento(@Param("medicamentoId") Long medicamentoId,
                                       @Param("hoje") LocalDate hoje);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Lote l "
            + "where l.medicamento.id = :medicamentoId "
            + "and l.quantidadeAtual > 0 "
            + "and l.validade >= :hoje "
            + "order by l.validade, l.id")
    List<Lote> buscarDisponiveisParaSaida(@Param("medicamentoId") Long medicamentoId,
                                          @Param("hoje") LocalDate hoje);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Lote l where l.id = :id")
    Optional<Lote> buscarPorIdComTravamento(@Param("id") Long id);

    @Query("select l from Lote l join fetch l.medicamento "
            + "where l.quantidadeAtual > 0 "
            + "and l.validade >= :inicio and l.validade <= :fim "
            + "order by l.validade, l.id")
    List<Lote> buscarVencendoEntre(@Param("inicio") LocalDate inicio,
                                   @Param("fim") LocalDate fim);

    @Query("select l from Lote l join fetch l.medicamento "
            + "where l.quantidadeAtual > 0 and l.validade < :hoje "
            + "order by l.validade, l.id")
    List<Lote> buscarVencidosComSaldo(@Param("hoje") LocalDate hoje);
}