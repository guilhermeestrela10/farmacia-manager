package com.farmacia.farmacia.dto;
import java.math.BigDecimal;
public record ItemReceitaResponse(Long medicamentoId, String medicamentoNome, BigDecimal preco, int quantidade, long dispensado, long restante) {}
