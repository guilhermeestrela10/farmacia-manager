package com.farmacia.farmacia.dto;
import java.time.Instant;
public record AtendimentoResponse(Long id, Long clienteId, String clienteNome, String profissional, String observacoes, Instant criadoEm) {}
