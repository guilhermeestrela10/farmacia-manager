package com.farmacia.farmacia.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.farmacia.farmacia.model.Fabricante;

public interface FabricanteRepository extends JpaRepository<Fabricante, Long> {

    boolean existsByNomeIgnoreCase(String nome);
}