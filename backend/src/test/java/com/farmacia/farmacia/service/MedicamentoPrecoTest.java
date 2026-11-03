package com.farmacia.farmacia.service;

import static org.junit.jupiter.api.Assertions.*;
import com.farmacia.farmacia.dto.MedicamentoRequest;
import com.farmacia.farmacia.model.TipoMedicamento;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MedicamentoPrecoTest {
    @Test
    void precoDeveCaberNoBancoSemArredondamentoSilencioso() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertFalse(validator.validate(request("10.005")).isEmpty());
            assertFalse(validator.validate(request("100000000.00")).isEmpty());
            assertTrue(validator.validate(request("10.01")).isEmpty());
        }
    }

    private MedicamentoRequest request(String preco) {
        return new MedicamentoRequest("Fictício", null, 1L, 1L,
                TipoMedicamento.GENERICO, new BigDecimal(preco), 0);
    }
}
