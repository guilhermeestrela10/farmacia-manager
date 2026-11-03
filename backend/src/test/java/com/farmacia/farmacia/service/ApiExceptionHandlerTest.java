package com.farmacia.farmacia.service;

import static org.junit.jupiter.api.Assertions.*;
import com.farmacia.farmacia.exception.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class ApiExceptionHandlerTest {
    @Test
    void conflitoNaoExibeDetalhesDoBanco() {
        var resposta = new ApiExceptionHandler().tratarConflitoDeDados(new DataIntegrityViolationException("SQL com dados privados"));
        assertEquals(409, resposta.getStatusCode().value());
        assertFalse(resposta.getBody().get("mensagem").contains("privados"));
    }

    @Test
    void overflowRetornaErroDeValidacao() {
        assertEquals(400, new ApiExceptionHandler().tratarLimiteNumerico(new ArithmeticException()).getStatusCode().value());
    }
}
