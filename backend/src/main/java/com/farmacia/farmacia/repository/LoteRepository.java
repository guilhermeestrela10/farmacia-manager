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

    @Query("select coalesce(sum(l.quantidadeAtual), 0L), "
            + "coalesce(sum(case when l.validade >= :hoje then l.quantidadeAtual else 0 end), 0L) "
            + "from Lote l where l.medicamento.id = :medicamentoId")
    List<Object[]> resumirSaldoPorMedicamento(@Param("medicamentoId") Long medicamentoId,
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

    @Query("select sum(l.quantidadeAtual), sum(l.quantidadeAtual * l.medicamento.preco) "
            + "from Lote l "
            + "where l.quantidadeAtual > 0 and l.validade >= :de and l.validade <= :ate")
    List<Object[]> resumoPorValidade(@Param("de") LocalDate de, @Param("ate") LocalDate ate);
    @Query("select l from Lote l join fetch l.medicamento m "
            + "where l.quantidadeAtual > 0 "
            + "and (lower(m.nome) like lower(concat('%', :busca, '%')) or lower(l.numeroLote) like lower(concat('%', :busca, '%'))) "
            + "and l.validade >= :inicio and l.validade <= :fim order by l.validade,l.id")
    List<Lote> buscarComSaldo(@Param("busca") String busca,@Param("inicio") LocalDate inicio,@Param("fim") LocalDate fim);
}
