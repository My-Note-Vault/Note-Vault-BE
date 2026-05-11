package com.example.workspace.common;

import com.example.common.exception.GlobalErrorFormat;
import com.example.workspace.workspace.command.application.AlreadyInWorkSpaceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

import static com.example.common.exception.GlobalErrorCode.INVALID_INPUT_ERROR;

@Slf4j
@RestControllerAdvice
public class WorkSpaceControllerAdvice {

    private static final String ALREADY_IN_WORKSPACE_MESSAGE = "이미 참여중인 WorkSpace 입니다";

    @ExceptionHandler(value = AlreadyInWorkSpaceException.class)
    public ResponseEntity<GlobalErrorFormat> handleGeneralServerExceptions(final Exception ex) {
        log.error("error: {}", ex.getMessage(), ex);

        GlobalErrorFormat error = GlobalErrorFormat.of(INVALID_INPUT_ERROR, ALREADY_IN_WORKSPACE_MESSAGE);
        return ResponseEntity.badRequest()
                .body(error);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNoSuchElementException(final NoSuchElementException ex) {
        log.error("error: {}", ex.getMessage(), ex);
        return ResponseEntity.badRequest()
                .body(ex.getMessage());
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleMethodArgumentNotValidException(
            final MethodArgumentNotValidException ex
    ) {
        log.error("error: {}", ex.getMessage(), ex);
        StringBuilder errorMessage = new StringBuilder();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errorMessage.append(fieldError.getField())
                    .append(": ")
                    .append(fieldError.getDefaultMessage())
                    .append("\n");
        }
        return ResponseEntity.badRequest()
                .body(errorMessage.toString());
    }
}
