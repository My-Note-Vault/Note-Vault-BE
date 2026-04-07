package com.example.common.exception;

import lombok.Getter;

@Getter
public enum GlobalErrorCode {

    BAD_REQUEST_ERROR("BAD_REQUEST_ERROR"),
    INVALID_INPUT_ERROR("INVALID_INPUT_ERROR"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR"),

    UNAUTHORIZED_ERROR("UNAUTHORIZED_ERROR"),
    FORBIDDEN_ERROR("FORBIDDEN_ERROR");

    private final String label;

    GlobalErrorCode(String label) {
        this.label = label;
    }
}
