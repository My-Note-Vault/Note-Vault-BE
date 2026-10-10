package com.example.search.chat.session;

import com.example.search.chat.SemanticChatService.Source;
import com.example.search.chat.api.ChatApiContract.*;
import com.example.search.chat.api.ChatErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ChatSessionRepository {
    private static final TypeReference<List<Source>> SOURCES_TYPE = new TypeReference<>() { };
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public ChatSessionRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public SessionResponse createSession(long memberId, String title) {
        return jdbc.queryForObject("""
                INSERT INTO chat_session (session_id, member_id, title)
                VALUES (:sessionId, :memberId, :title) RETURNING *
                """, Map.of("sessionId", UUID.randomUUID(), "memberId", memberId, "title", title),
                (rs, row) -> readSession(rs));
    }

    public List<SessionResponse> sessions(long memberId, ChatCursor.SessionPosition position, int limit) {
        var parameters = new MapSqlParameterSource("memberId", memberId).addValue("limit", limit);
        String after = "";
        if (position != null) {
            after = " AND (updated_at, session_id) < (:updatedAt, :sessionId)";
            parameters.addValue("updatedAt", Timestamp.from(position.updatedAt()))
                    .addValue("sessionId", position.sessionId());
        }
        return jdbc.query("SELECT * FROM chat_session WHERE member_id = :memberId" + after
                        + " ORDER BY updated_at DESC, session_id DESC LIMIT :limit",
                parameters, (rs, row) -> readSession(rs));
    }

    public Optional<SessionResponse> session(long memberId, UUID sessionId, boolean forUpdate) {
        return jdbc.query("SELECT * FROM chat_session WHERE session_id = :sessionId AND member_id = :memberId"
                        + (forUpdate ? " FOR UPDATE" : ""),
                Map.of("sessionId", sessionId, "memberId", memberId), (rs, row) -> readSession(rs)).stream().findFirst();
    }

    /** Transaction-scoped across all API instances; hashes can only cause extra serialization. */
    public void lockRequest(long memberId, UUID requestId) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(:key, 0))",
                Map.of("key", "chat-request:" + memberId + ":" + requestId), (rs, row) -> true);
    }

    public Optional<StoredRun> run(long memberId, UUID runId) {
        return jdbc.query("SELECT * FROM chat_run WHERE run_id = :runId AND member_id = :memberId",
                Map.of("runId", runId, "memberId", memberId), (rs, row) -> readRun(rs)).stream().findFirst();
    }

    public Optional<StoredRun> request(long memberId, UUID requestId) {
        return jdbc.query("SELECT * FROM chat_run WHERE member_id = :memberId AND request_id = :requestId",
                Map.of("memberId", memberId, "requestId", requestId), (rs, row) -> readRun(rs)).stream().findFirst();
    }

    public boolean hasActiveRun(UUID sessionId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM chat_run WHERE session_id = :sessionId
                               AND status IN ('QUEUED', 'RUNNING'))
                """, Map.of("sessionId", sessionId), Boolean.class));
    }

    public StoredRun createRun(long memberId, UUID sessionId, UUID requestId, String question) {
        return jdbc.queryForObject("""
                INSERT INTO chat_run (run_id, member_id, session_id, request_id, question)
                VALUES (:runId, :memberId, :sessionId, :requestId, :question) RETURNING *
                """, Map.of("runId", UUID.randomUUID(), "memberId", memberId, "sessionId", sessionId,
                "requestId", requestId, "question", question), (rs, row) -> readRun(rs));
    }

    /** The caller holds the session lock; allocating both positions rolls back with the run. */
    public void createMessages(StoredRun run) {
        Long firstSequence = jdbc.queryForObject("""
                UPDATE chat_session
                SET next_message_sequence = next_message_sequence + 2,
                    updated_at = GREATEST(updated_at, clock_timestamp())
                WHERE session_id = :sessionId RETURNING next_message_sequence - 2
                """, Map.of("sessionId", run.sessionId()), Long.class);
        jdbc.update("""
                INSERT INTO chat_message (message_id, session_id, run_id, role, content, sequence, status)
                VALUES (:userId, :sessionId, :runId, 'USER', :question, :userSequence, 'COMPLETED'),
                       (:assistantId, :sessionId, :runId, 'ASSISTANT', '', :assistantSequence, 'PENDING')
                """, Map.of("userId", UUID.randomUUID(), "assistantId", UUID.randomUUID(),
                "sessionId", run.sessionId(), "runId", run.runId(), "question", run.question(),
                "userSequence", firstSequence, "assistantSequence", firstSequence + 1));
    }

    public List<MessageResponse> messages(long memberId, UUID sessionId, long afterSequence, int limit) {
        return jdbc.query("""
                SELECT m.* FROM chat_message m
                JOIN chat_session s ON s.session_id = m.session_id
                WHERE s.member_id = :memberId AND m.session_id = :sessionId AND m.sequence > :after
                ORDER BY m.sequence LIMIT :limit
                """, Map.of("memberId", memberId, "sessionId", sessionId, "after", afterSequence, "limit", limit),
                (rs, row) -> new MessageResponse(rs.getObject("message_id", UUID.class),
                        rs.getObject("run_id", UUID.class), MessageRole.valueOf(rs.getString("role")),
                        rs.getString("content"), readSources(rs.getString("sources")), rs.getLong("sequence"),
                        MessageStatus.valueOf(rs.getString("status")), instant(rs, "created_at")));
    }

    public void startRun(UUID runId) {
        requireUpdated(jdbc.update("""
                UPDATE chat_run SET status = 'RUNNING', started_at = GREATEST(created_at, clock_timestamp())
                WHERE run_id = :runId AND status = 'QUEUED'
                """, Map.of("runId", runId)));
    }

    public void finishRun(UUID runId, RunStatus status, FinishReason reason, ChatErrorCode error, Instant resetAt) {
        var parameters = new MapSqlParameterSource("runId", runId)
                .addValue("status", status.name()).addValue("reason", reason == null ? null : reason.name())
                .addValue("error", error == null ? null : error.name())
                .addValue("resetAt", resetAt == null ? null : Timestamp.from(resetAt));
        requireUpdated(jdbc.update("""
                UPDATE chat_run SET status = :status, finish_reason = :reason, error_code = :error,
                    error_reset_at = :resetAt,
                    finished_at = GREATEST(created_at, started_at, clock_timestamp())
                WHERE run_id = :runId AND status IN ('QUEUED', 'RUNNING')
                """, parameters));
    }

    public void finishAnswer(UUID runId, String content, List<Source> sources, MessageStatus status) {
        requireUpdated(jdbc.update("""
                UPDATE chat_message SET content = :content, sources = CAST(:sources AS jsonb), status = :status
                WHERE run_id = :runId AND role = 'ASSISTANT' AND status = 'PENDING'
                """, Map.of("runId", runId, "content", content, "sources", writeSources(sources), "status", status.name())));
    }

    public void cancelAnswer(UUID runId) {
        requireUpdated(jdbc.update("""
                UPDATE chat_message SET status = 'INCOMPLETE'
                WHERE run_id = :runId AND role = 'ASSISTANT' AND status = 'PENDING'
                """, Map.of("runId", runId)));
    }

    public void touchSession(UUID sessionId) {
        requireUpdated(jdbc.update("""
                UPDATE chat_session SET updated_at = GREATEST(updated_at, clock_timestamp())
                WHERE session_id = :sessionId
                """, Map.of("sessionId", sessionId)));
    }

    private void requireUpdated(int rows) {
        if (rows != 1) throw new IllegalStateException("Chat state changed without holding the session lock");
    }

    private SessionResponse readSession(ResultSet rs) throws SQLException {
        return new SessionResponse(rs.getObject("session_id", UUID.class), rs.getString("title"),
                instant(rs, "created_at"), instant(rs, "updated_at"));
    }

    private StoredRun readRun(ResultSet rs) throws SQLException {
        String reason = rs.getString("finish_reason");
        String error = rs.getString("error_code");
        return new StoredRun(rs.getObject("run_id", UUID.class), rs.getObject("session_id", UUID.class),
                rs.getLong("member_id"), rs.getObject("request_id", UUID.class), rs.getString("question"),
                RunStatus.valueOf(rs.getString("status")), reason == null ? null : FinishReason.valueOf(reason),
                error == null ? null : ChatErrorCode.valueOf(error), instant(rs, "error_reset_at"),
                instant(rs, "created_at"), instant(rs, "started_at"), instant(rs, "finished_at"));
    }

    private Instant instant(ResultSet rs, String name) throws SQLException {
        Timestamp value = rs.getTimestamp(name);
        return value == null ? null : value.toInstant();
    }

    private String writeSources(List<Source> sources) {
        try {
            return mapper.writeValueAsString(sources);
        } catch (JsonProcessingException invalid) {
            throw new IllegalStateException("Cannot serialize chat sources", invalid);
        }
    }

    private List<Source> readSources(String json) {
        try {
            return mapper.readValue(json, SOURCES_TYPE);
        } catch (JsonProcessingException invalid) {
            throw new IllegalStateException("Cannot read stored chat sources", invalid);
        }
    }

    public record StoredRun(UUID runId, UUID sessionId, long memberId, UUID requestId, String question,
                            RunStatus status, FinishReason finishReason, ChatErrorCode errorCode, Instant errorResetAt,
                            Instant createdAt, Instant startedAt, Instant finishedAt) {
        public boolean active() {
            return status == RunStatus.QUEUED || status == RunStatus.RUNNING;
        }

        public RunResponse response() {
            ErrorResponse error = errorCode == null ? null
                    : new ErrorResponse(errorCode, errorCode.message(), runId, errorResetAt);
            return new RunResponse(sessionId, runId, status, finishReason, error, createdAt, startedAt, finishedAt);
        }
    }
}
