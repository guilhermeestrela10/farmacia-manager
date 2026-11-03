package com.farmacia.farmacia.exception;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> tratarConflitoDeDados(DataIntegrityViolationException ex) {
        return ResponseEntity.status(409).body(Map.of("mensagem",
                "Dados em conflito com outro cadastro ou operação. Atualize a página e confira os valores."));
    }

    @ExceptionHandler(ArithmeticException.class)
    public ResponseEntity<Map<String, String>> tratarLimiteNumerico(ArithmeticException ex) {
        return ResponseEntity.badRequest().body(Map.of("mensagem", "Valor ou quantidade excede o limite permitido."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> tratarErroDeNegocio(ResponseStatusException ex) {
        String mensagem = ex.getReason() != null
                ? ex.getReason()
                : "Não foi possível concluir a operação.";

        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("mensagem", mensagem));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> tratarValidacao(
            MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(" "));

        if (mensagem.isBlank()) {
            mensagem = "Dados inválidos.";
        }

        return ResponseEntity.badRequest().body(Map.of("mensagem", mensagem));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> tratarCorpoInvalido(
            HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("mensagem", "Corpo da requisição inválido."));
    }
}
