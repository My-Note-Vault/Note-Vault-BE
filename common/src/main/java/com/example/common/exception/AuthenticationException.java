package com.example.common.exception;

import lombok.Getter;

@Getter
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }
}
