package com.example.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.example.common.exception.GlobalErrorCode.*;

@Slf4j
@RestControllerAdvice
public class GlobalControllerAdvice {


    @ExceptionHandler(value = IllegalArgumentException.class)
    public ResponseEntity<GlobalErrorFormat> handleGeneralServerExceptions(final Exception ex) {
        log.error("error: {}", ex.getMessage(), ex);

        GlobalErrorFormat error = GlobalErrorFormat.of(INVALID_INPUT_ERROR, ex.getMessage());
        return ResponseEntity.internalServerError()
                .body(error);
    }

    @ExceptionHandler(value = AuthenticationException.class)
    public ResponseEntity<GlobalErrorFormat> handleAuthenticationExceptions(final Exception ex) {
        log.error("error: {}", ex.getMessage(), ex);

        GlobalErrorFormat error = GlobalErrorFormat.of(AUTHENTICATION_ERROR, ex.getMessage());
        return ResponseEntity.internalServerError()
                .body(error);
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<GlobalErrorFormat> handleRemainExceptions(final Exception ex) {
        log.error("error: {}", ex.getMessage(), ex);

        GlobalErrorFormat error = GlobalErrorFormat.of(BAD_REQUEST_ERROR, ex.getMessage());
        return ResponseEntity.internalServerError()
                .body(error);
    }
}
