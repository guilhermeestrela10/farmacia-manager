package com.farmacia.farmacia.repository;
import com.farmacia.farmacia.model.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
import jakarta.persistence.LockModeType;
public interface ClienteRepository extends JpaRepository<Cliente,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from Cliente c where c.id=:id")
    Optional<Cliente> buscarPorIdComTravamento(@Param("id") Long id);
    @Query("select c from Cliente c where lower(c.nome) like lower(concat('%', :busca, '%')) order by c.nome,c.id")
    List<Cliente> buscar(@Param("busca") String busca);
}
