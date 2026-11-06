package com.farmacia.farmacia.repository;
import com.farmacia.farmacia.model.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
import jakarta.persistence.LockModeType;
public interface DispensacaoRepository extends JpaRepository<Dispensacao,Long> {
    @Query("select coalesce(sum(i.quantidade),0L) from Dispensacao d join d.venda v join v.itens i where d.receita.id=:receitaId and i.medicamento.id=:medicamentoId and v.status=com.farmacia.farmacia.model.StatusVenda.CONCLUIDA")
    long quantidadeDispensada(@Param("receitaId") Long receitaId,@Param("medicamentoId") Long medicamentoId);
    List<Dispensacao> findAllByOrderByCriadaEmDescIdDesc();
}
