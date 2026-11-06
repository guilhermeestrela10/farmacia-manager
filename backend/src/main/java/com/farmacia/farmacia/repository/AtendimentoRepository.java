package com.farmacia.farmacia.repository;
import com.farmacia.farmacia.model.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
import jakarta.persistence.LockModeType;
public interface AtendimentoRepository extends JpaRepository<Atendimento,Long> {
    @Query("select a from Atendimento a join fetch a.cliente where (:clienteId is null or a.cliente.id=:clienteId) order by a.criadoEm desc,a.id desc")
    List<Atendimento> listar(@Param("clienteId") Long clienteId);
}
