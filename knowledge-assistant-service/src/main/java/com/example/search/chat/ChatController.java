package com.example.search.chat;

import com.example.common.AuthMemberId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@Slf4j
public class ChatController {
    private final SemanticChatService service;

    @PostMapping("/api/v1/chat")
    public ResponseEntity<SemanticChatService.ChatResult> chat(@Valid @RequestBody ChatRequest request,
                                                               @AuthMemberId Long memberId) {
        return ResponseEntity.ok(service.chat(memberId, request.question()));
    }

    @PostMapping(value = "/api/v1/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream(@Valid @RequestBody ChatRequest request, @AuthMemberId Long memberId) {
        SseEmitter emitter = new SseEmitter(120_000L);
        Thread.startVirtualThread(() -> stream(memberId, request.question(), emitter));
        return ResponseEntity.ok()
                .header("Cache-Control", "no-cache")
                .header("X-Accel-Buffering", "no")
                .body(emitter);
    }

    private void stream(Long memberId, String question, SseEmitter emitter) {
        String stage = "PREPARING";
        try {
            send(emitter, "start", Map.of());
            SemanticChatService.ChatPreparation preparation = service.prepare(memberId, question);
            if (!preparation.hasContext()) {
                send(emitter, "delta", Map.of("text", "관련 문서 내용을 찾지 못했습니다."));
                send(emitter, "done", Map.of("status", "NO_CONTEXT"));
                emitter.complete();
                return;
            }

            send(emitter, "sources", preparation.sources());
            stage = "GENERATING";
            service.streamAnswer(preparation, delta -> sendUnchecked(emitter, "delta", Map.of("text", delta)));
            send(emitter, "done", Map.of("status", "ANSWERED"));
            emitter.complete();
        } catch (Exception exception) {
            log.error("Chat SSE failed at stage={} memberId={}", stage, memberId, exception);
            try {
                String message = "PREPARING".equals(stage)
                        ? "문서 검색을 준비하는 중 오류가 발생했습니다."
                        : "답변을 생성하는 중 오류가 발생했습니다.";
                send(emitter, "error", Map.of("message", message, "stage", stage));
                emitter.complete();
            } catch (Exception ignored) {
                emitter.completeWithError(exception);
            }
        }
    }

    private void send(SseEmitter emitter, String name, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(name).data(data, MediaType.APPLICATION_JSON));
    }

    private void sendUnchecked(SseEmitter emitter, String name, Object data) {
        try {
            send(emitter, name, data);
        } catch (IOException exception) {
            throw new IllegalStateException("SSE 연결이 종료되었습니다.", exception);
        }
    }

    public record ChatRequest(@NotBlank @Size(max = 4000) String question) {
    }
}
