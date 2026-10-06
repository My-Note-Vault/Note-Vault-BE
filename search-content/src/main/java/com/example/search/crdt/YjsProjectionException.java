package com.example.search.crdt;

public final class YjsProjectionException extends RuntimeException {
    public enum Reason { WAITING_DEPENDENCIES, INPUT_LIMIT, OUTPUT_LIMIT, TIMEOUT, INVALID_RESULT, RUNTIME }

    private final Reason reason;

    public YjsProjectionException(Reason reason) {
        this(reason, null);
    }

    public YjsProjectionException(Reason reason, Throwable cause) {
        super("Yjs projection failed: " + reason, cause);
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}
