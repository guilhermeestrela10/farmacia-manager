package com.farmacia.farmacia.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.farmacia.farmacia.model.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    boolean existsByNomeIgnoreCase(String nome);
}