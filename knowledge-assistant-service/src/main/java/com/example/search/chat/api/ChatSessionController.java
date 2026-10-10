package com.example.search.chat.api;

import com.example.common.AuthMemberId;
import com.example.search.chat.api.ChatApiContract.*;
import com.example.search.chat.session.ChatSessionService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatSessionController {
    private final ChatSessionService service;

    public ChatSessionController(ChatSessionService service) {
        this.service = service;
    }

    @PostMapping("/sessions")
    public ResponseEntity<SessionResponse> createSession(@AuthMemberId Long memberId,
                                                        @Valid @RequestBody CreateSessionRequest request) {
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore())
                .body(service.createSession(memberId, request.title()));
    }

    @GetMapping("/sessions")
    public ResponseEntity<CursorPage<SessionResponse>> sessions(@AuthMemberId Long memberId,
            @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.sessions(memberId, cursor, limit));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<CursorPage<MessageResponse>> messages(@AuthMemberId Long memberId, @PathVariable UUID sessionId,
            @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.messages(memberId, sessionId, cursor, limit));
    }

    @PostMapping("/sessions/{sessionId}/runs")
    public ResponseEntity<RunAcceptedResponse> createRun(@AuthMemberId Long memberId, @PathVariable UUID sessionId,
                                                        @Valid @RequestBody CreateRunRequest request) {
        RunAcceptedResponse accepted = service.createRun(memberId, sessionId, request.requestId(), request.question());
        return ResponseEntity.accepted().location(URI.create("/api/v1/chat/runs/" + accepted.runId()))
                .cacheControl(CacheControl.noStore()).body(accepted);
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<RunResponse> run(@AuthMemberId Long memberId, @PathVariable UUID runId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.run(memberId, runId));
    }

    @PostMapping("/runs/{runId}/cancel")
    public ResponseEntity<RunResponse> cancel(@AuthMemberId Long memberId, @PathVariable UUID runId) {
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(service.cancel(memberId, runId));
    }
}
