package com.example.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class GlobalErrorFormat {

    private final GlobalErrorCode code;
    private final String message;

    public static GlobalErrorFormat of(
            final GlobalErrorCode code,
            final String message
    ) {
        return new GlobalErrorFormat(code, message);
    }

}
