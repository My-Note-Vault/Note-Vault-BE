package com.example.search.chat.session;

import com.example.search.chat.SemanticChatService.Source;
import com.example.search.chat.api.ChatApiContract.*;
import com.example.search.chat.api.ChatErrorCode;
import com.example.search.chat.api.ChatException;
import com.example.search.chat.policy.ChatPolicy;
import com.example.search.chat.session.ChatSessionRepository.StoredRun;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Transactions end before any future model/tool call. No external calls belong in this service. */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class ChatSessionService {
    private final ChatSessionRepository repository;

    public ChatSessionService(ChatSessionRepository repository) {
        this.repository = repository;
    }

    public SessionResponse createSession(long memberId, String title) {
        if (title != null && (title.length() > ChatPolicy.MAX_SESSION_TITLE_LENGTH || title.indexOf('\0') >= 0)) {
            throw new ChatException(ChatErrorCode.INVALID_CHAT_REQUEST);
        }
        String normalized = title == null || title.isBlank() ? "새 대화" : title.strip();
        return repository.createSession(memberId, normalized);
    }

    @Transactional(readOnly = true)
    public CursorPage<SessionResponse> sessions(long memberId, String cursor, int limit) {
        validateLimit(limit);
        List<SessionResponse> found = repository.sessions(memberId, ChatCursor.session(cursor, memberId), limit + 1);
        boolean hasMore = found.size() > limit;
        List<SessionResponse> items = hasMore ? found.subList(0, limit) : found;
        String nextCursor = null;
        if (hasMore) {
            SessionResponse last = items.getLast();
            nextCursor = ChatCursor.session(memberId, last.updatedAt(), last.sessionId());
        }
        return new CursorPage<>(items, nextCursor);
    }

    @Transactional(readOnly = true)
    public CursorPage<MessageResponse> messages(long memberId, UUID sessionId, String cursor, int limit) {
        requireSession(memberId, sessionId, false);
        validateLimit(limit);
        long after = ChatCursor.message(cursor, memberId, sessionId);
        List<MessageResponse> found = repository.messages(memberId, sessionId, after, limit + 1);
        boolean hasMore = found.size() > limit;
        List<MessageResponse> items = hasMore ? found.subList(0, limit) : found;
        String nextCursor = hasMore ? ChatCursor.message(memberId, sessionId, items.getLast().sequence()) : null;
        return new CursorPage<>(items, nextCursor);
    }

    public RunAcceptedResponse createRun(long memberId, UUID sessionId, UUID requestId, String question) {
        if (sessionId == null || requestId == null) throw new ChatException(ChatErrorCode.INVALID_CHAT_REQUEST);
        String normalized = ChatPolicy.normalizeQuestion(question);
        if (normalized.indexOf('\0') >= 0) throw new ChatException(ChatErrorCode.INVALID_CHAT_REQUEST);

        // Always acquire request lock before session lock, including retries in a different session.
        repository.lockRequest(memberId, requestId);
        requireSession(memberId, sessionId, true);
        Optional<StoredRun> previous = repository.request(memberId, requestId);
        if (previous.isPresent()) {
            StoredRun run = previous.get();
            if (!sessionId.equals(run.sessionId()) || !normalized.equals(run.question())) {
                throw new ChatException(ChatErrorCode.REQUEST_ID_CONFLICT);
            }
            return accepted(run);
        }
        if (repository.hasActiveRun(sessionId)) throw new ChatException(ChatErrorCode.SESSION_BUSY);
        StoredRun run = repository.createRun(memberId, sessionId, requestId, normalized);
        repository.createMessages(run);
        return accepted(run);
    }

    @Transactional(readOnly = true)
    public RunResponse run(long memberId, UUID runId) {
        return requireRun(memberId, runId).response();
    }

    public RunResponse cancel(long memberId, UUID runId) {
        StoredRun run = lockRun(memberId, runId);
        if (!run.active()) return run.response();
        repository.finishRun(runId, RunStatus.CANCELLED, FinishReason.USER_CANCELLED, null, null);
        repository.cancelAnswer(runId);
        repository.touchSession(run.sessionId());
        return requireRun(memberId, runId).response();
    }

    /** Only the caller that changes QUEUED to RUNNING may invoke the model. */
    public Optional<RunTask> claimRun(long memberId, UUID runId) {
        StoredRun run = lockRun(memberId, runId);
        if (run.status() != RunStatus.QUEUED) return Optional.empty();
        repository.startRun(runId);
        return Optional.of(new RunTask(run.sessionId(), runId, memberId, run.question()));
    }

    public boolean completeRun(long memberId, UUID runId, FinishReason reason, String answer, List<Source> sources) {
        if (reason == null || reason == FinishReason.USER_CANCELLED) {
            throw new IllegalArgumentException("A completed run requires an answer finish reason");
        }
        Objects.requireNonNull(answer, "answer");
        List<Source> snapshot = List.copyOf(sources);
        StoredRun run = lockRun(memberId, runId);
        if (run.status() != RunStatus.RUNNING) return false;
        repository.finishRun(runId, RunStatus.COMPLETED, reason, null, null);
        repository.finishAnswer(runId, answer, snapshot, MessageStatus.COMPLETED);
        repository.touchSession(run.sessionId());
        return true;
    }

    public boolean failRun(long memberId, UUID runId, ChatException failure, String partialAnswer, List<Source> sources) {
        Objects.requireNonNull(failure, "failure");
        Objects.requireNonNull(partialAnswer, "partialAnswer");
        List<Source> snapshot = List.copyOf(sources);
        StoredRun run = lockRun(memberId, runId);
        if (!run.active()) return false;
        repository.finishRun(runId, RunStatus.FAILED, null, failure.code(), failure.resetAt());
        repository.finishAnswer(runId, partialAnswer, snapshot, MessageStatus.INCOMPLETE);
        repository.touchSession(run.sessionId());
        return true;
    }

    private StoredRun lockRun(long memberId, UUID runId) {
        StoredRun run = requireRun(memberId, runId);
        // Same lock order as createRun: all run/message mutations hold their session row lock.
        requireSession(memberId, run.sessionId(), true);
        return requireRun(memberId, runId);
    }

    private StoredRun requireRun(long memberId, UUID runId) {
        if (runId == null) throw new ChatException(ChatErrorCode.INVALID_CHAT_REQUEST);
        return repository.run(memberId, runId).orElseThrow(() -> new ChatException(ChatErrorCode.RUN_NOT_FOUND));
    }

    private void requireSession(long memberId, UUID sessionId, boolean lock) {
        if (sessionId == null) throw new ChatException(ChatErrorCode.INVALID_CHAT_REQUEST);
        repository.session(memberId, sessionId, lock)
                .orElseThrow(() -> new ChatException(ChatErrorCode.SESSION_NOT_FOUND));
    }

    private RunAcceptedResponse accepted(StoredRun run) {
        return new RunAcceptedResponse(run.sessionId(), run.runId(), run.status());
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > 100) throw new ChatException(ChatErrorCode.INVALID_CHAT_REQUEST);
    }

    public record RunTask(UUID sessionId, UUID runId, long memberId, String question) { }
}
