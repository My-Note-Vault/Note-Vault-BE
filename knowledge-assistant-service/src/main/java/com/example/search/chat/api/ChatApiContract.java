package com.example.search.chat.api;

import com.example.search.chat.policy.ChatPolicy;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** V1 wire types. Session, run, usage and event endpoints are introduced in later steps. */
public final class ChatApiContract {
    private ChatApiContract() {
    }

    public record CreateSessionRequest(
            @Size(max = ChatPolicy.MAX_SESSION_TITLE_LENGTH) String title) {
    }

    /** sessionId comes from the URL; the authenticated user ID is never accepted in the body. */
    public record CreateRunRequest(
            @NotNull UUID requestId,
            @NotBlank @Size(max = ChatPolicy.MAX_QUESTION_LENGTH) String question) {
    }

    public record RunAcceptedResponse(UUID sessionId, UUID runId, RunStatus status) {
    }

    public enum RunStatus {
        QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED
    }

    /** For failed runs, error.code carries the failure reason. */
    public enum FinishReason {
        ANSWERED, NO_CONTEXT, NEEDS_USER_INPUT, USER_CANCELLED
    }

    public record RunResponse(UUID sessionId, UUID runId, RunStatus status,
                              FinishReason finishReason, ErrorResponse error,
                              Instant createdAt, Instant startedAt, Instant finishedAt) {
    }

    /** remaining = max(0, limit - used - reserved); resetAt is an absolute UTC instant. */
    public record UsageResponse(LocalDate quotaDate, String timezone, long limit, long used,
                                long reserved, long remaining, Instant resetAt) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorResponse(ChatErrorCode code, String message, UUID runId, Instant resetAt) {
        public static ErrorResponse of(ChatErrorCode code) {
            return new ErrorResponse(code, code.message(), null, null);
        }

        public static ErrorResponse from(ChatException exception) {
            return new ErrorResponse(exception.code(), exception.getMessage(), null, exception.resetAt());
        }
    }

    public enum EventType {
        RUN_STARTED("run_started"),
        TOOL_STARTED("tool_started"),
        TOOL_FINISHED("tool_finished"),
        DELTA("delta"),
        SOURCES("sources"),
        USAGE("usage"),
        COMPLETED("completed"),
        ERROR("error"),
        CANCELLED("cancelled");

        private final String wireName;

        EventType(String wireName) {
            this.wireName = wireName;
        }

        @JsonValue
        public String wireName() {
            return wireName;
        }
    }

    /** sequence starts at 1 per run and is also the SSE id used for reconnects. */
    public record RunEvent<T>(UUID runId, long sequence, EventType type, Instant occurredAt, T data) {
    }

    public record DeltaPayload(String text) {
    }

    public record ToolStartedPayload(String callId, String toolName) {
    }

    public record ToolFinishedPayload(String callId, String toolName, boolean succeeded, ChatErrorCode errorCode) {
    }
}
