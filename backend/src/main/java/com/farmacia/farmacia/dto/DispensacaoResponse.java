package com.farmacia.farmacia.dto;
import com.farmacia.farmacia.model.StatusVenda;
import java.math.BigDecimal;
import java.time.Instant;
public record DispensacaoResponse(Long id, Long receitaId, String clienteNome, Long vendaId, BigDecimal total, StatusVenda statusVenda, Instant criadaEm) {}
