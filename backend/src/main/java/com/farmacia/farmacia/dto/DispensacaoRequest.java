package com.farmacia.farmacia.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.farmacia.farmacia.model.FormaPagamento;
import java.util.List;
public record DispensacaoRequest(@NotNull Long receitaId, @NotNull FormaPagamento formaPagamento,
    @NotEmpty @Size(max=20) List<@NotNull @Valid ItemVendaRequest> itens) {}
