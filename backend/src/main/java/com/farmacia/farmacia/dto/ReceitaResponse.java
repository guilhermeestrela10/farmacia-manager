package com.farmacia.farmacia.dto;
import java.time.LocalDate;
import java.util.List;
public record ReceitaResponse(Long id, Long clienteId, String clienteNome, String prescritor, String numero, LocalDate emitidaEm, LocalDate validadeAte, String observacoes, List<ItemReceitaResponse> itens) {}
