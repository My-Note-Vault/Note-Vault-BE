package com.example.search.chat.api;

import java.time.Instant;
import java.util.Objects;

public class ChatException extends RuntimeException {
    private final ChatErrorCode code;
    private final Instant resetAt;

    public ChatException(ChatErrorCode code) {
        this(code, null);
    }

    public ChatException(ChatErrorCode code, Instant resetAt) {
        super(Objects.requireNonNull(code).message());
        this.code = code;
        this.resetAt = resetAt;
    }

    public ChatErrorCode code() {
        return code;
    }

    public Instant resetAt() {
        return resetAt;
    }
}
