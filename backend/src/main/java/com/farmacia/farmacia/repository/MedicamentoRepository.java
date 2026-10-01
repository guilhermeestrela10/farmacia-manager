package com.farmacia.farmacia.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.farmacia.farmacia.model.Medicamento;

public interface MedicamentoRepository extends JpaRepository<Medicamento, Long> {

    boolean existsByCodigoBarras(String codigoBarras);

    List<Medicamento> findByNomeContainingIgnoreCaseOrderByNome(String nome);

    List<Medicamento> findAllByOrderByNome();
}