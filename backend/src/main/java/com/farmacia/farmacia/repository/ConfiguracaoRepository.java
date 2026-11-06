package com.farmacia.farmacia.repository;
import com.farmacia.farmacia.model.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
import jakarta.persistence.LockModeType;
public interface ConfiguracaoRepository extends JpaRepository<Configuracao,Long> {}
