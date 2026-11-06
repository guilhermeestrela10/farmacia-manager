package com.farmacia.farmacia.repository;
import com.farmacia.farmacia.model.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
import jakarta.persistence.LockModeType;
public interface ReceitaRepository extends JpaRepository<Receita,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from Receita r where r.id=:id")
    Optional<Receita> buscarPorIdComTravamento(@Param("id") Long id);
    @Query("select r from Receita r join fetch r.cliente where (:clienteId is null or r.cliente.id=:clienteId) order by r.id desc")
    List<Receita> listar(@Param("clienteId") Long clienteId);
}
