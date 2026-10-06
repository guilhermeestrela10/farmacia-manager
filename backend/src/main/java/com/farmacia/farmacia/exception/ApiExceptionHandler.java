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

@RestControllerAdvice
public class ApiExceptionHandler {

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