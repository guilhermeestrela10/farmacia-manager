package com.farmacia.farmacia.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.farmacia.farmacia.model.Venda;

import jakarta.persistence.LockModeType;

public interface VendaRepository
        extends JpaRepository<Venda, Long>, JpaSpecificationExecutor<Venda> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Venda v where v.id = :id")
    Optional<Venda> buscarPorIdComTravamento(@Param("id") Long id);
}