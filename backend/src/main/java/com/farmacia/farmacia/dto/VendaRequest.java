package com.farmacia.farmacia.dto;

import java.util.List;

import com.farmacia.farmacia.model.FormaPagamento;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VendaRequest(
        @NotEmpty(message = "A venda precisa ter ao menos um item.")
        @Size(max = 50, message = "Uma venda pode ter no máximo 50 itens.")
        @Valid
        List<ItemVendaRequest> itens,

        @NotNull(message = "A forma de pagamento é obrigatória.")
        FormaPagamento formaPagamento
) {
}