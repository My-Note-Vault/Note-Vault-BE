package com.example.search.chat.api;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.example.search.chat")
public class ChatControllerAdvice {

    @ExceptionHandler(ChatException.class)
    public ResponseEntity<ChatApiContract.ErrorResponse> handleChatException(ChatException exception) {
        return ResponseEntity.status(exception.code().httpStatus())
                .body(ChatApiContract.ErrorResponse.from(exception));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ChatApiContract.ErrorResponse> handleInvalidRequest(Exception exception) {
        return ResponseEntity.badRequest()
                .body(ChatApiContract.ErrorResponse.of(ChatErrorCode.INVALID_CHAT_REQUEST));
    }
}
