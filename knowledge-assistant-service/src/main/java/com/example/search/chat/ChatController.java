package com.example.search.chat;

import com.example.common.AuthMemberId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class ChatController {
    private final SemanticChatService service;

    @PostMapping("/api/v1/chat")
    public ResponseEntity<SemanticChatService.ChatResult> chat(@Valid @RequestBody ChatRequest request,
                                                               @AuthMemberId Long memberId) {
        return ResponseEntity.ok(service.chat(memberId, request.question()));
    }

    public record ChatRequest(@NotBlank @Size(max = 4000) String question) {
    }
}
