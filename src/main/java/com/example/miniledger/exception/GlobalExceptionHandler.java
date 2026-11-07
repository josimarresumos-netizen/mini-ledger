package com.example.miniledger.exception;

import com.example.miniledger.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException e) {
        // Erros de validação são seguros de retornar (ex: "amount deve ser > 0")
        return build(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> missingHeader(MissingRequestHeaderException e) {
        return build(HttpStatus.BAD_REQUEST, "Header obrigatório ausente: " + e.getHeaderName());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> unprocessable(IllegalStateException e) {
        // Erros de regra de negócio (ex: "Saldo insuficiente") também são seguros
        return build(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> generic(Exception e) {
        // SEGURANÇA: Loga o erro real no servidor, mas não vaza para o cliente
        log.error("Erro interno não tratado", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno no servidor. Contate o suporte.");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String msg) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), status.getReasonPhrase(), msg, Instant.now()));
    }
}
