package com.farmacia.farmacia.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.farmacia.farmacia.model.MovimentacaoEstoque;
import com.farmacia.farmacia.model.TipoMovimentacao;

public interface MovimentacaoEstoqueRepository
        extends JpaRepository<MovimentacaoEstoque, Long>,
        JpaSpecificationExecutor<MovimentacaoEstoque> {

    List<MovimentacaoEstoque> findByVendaIdAndTipo(Long vendaId, TipoMovimentacao tipo);
}