package com.farmacia.farmacia.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.farmacia.farmacia.model.MovimentacaoEstoque;

public interface MovimentacaoEstoqueRepository
        extends JpaRepository<MovimentacaoEstoque, Long> {
}