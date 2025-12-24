package com.example.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalControllerAdvice {

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<String> handleException(final Exception ex) {
        log.error("error: {}", ex.getMessage(), ex);
        return ResponseEntity.internalServerError()
                .body(ex.getMessage());
    }
}
