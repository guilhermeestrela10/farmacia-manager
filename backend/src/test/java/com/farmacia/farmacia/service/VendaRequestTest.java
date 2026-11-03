package com.farmacia.farmacia.service;

import static org.junit.jupiter.api.Assertions.*;
import com.farmacia.farmacia.dto.VendaRequest;
import com.farmacia.farmacia.model.FormaPagamento;
import jakarta.validation.Validation;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class VendaRequestTest {
    @Test
    void rejeitaItemNuloSemChegarAoServico() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertFalse(factory.getValidator().validate(new VendaRequest(Arrays.asList((com.farmacia.farmacia.dto.ItemVendaRequest) null), FormaPagamento.PIX)).isEmpty());
        }
    }
}
