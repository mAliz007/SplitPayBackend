package com.SplitPay.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        // This sends the actual message (e.g., "USER_ALREADY_EXISTS") to the frontend
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}