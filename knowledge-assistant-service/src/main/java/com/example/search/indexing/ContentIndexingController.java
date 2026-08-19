package com.example.search.indexing;

import com.example.common.AuthMemberId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class ContentIndexingController {
    private final ContentIndexingService service;

    @PostMapping("/api/v1/documents/{type}/{resourceId}/indexing")
    public ResponseEntity<Void> indexDocument(@PathVariable String type,
                                              @PathVariable Long resourceId,
                                              @RequestBody DocumentIndexingRequest request,
                                              @AuthMemberId Long memberId) {
        service.indexDocument(memberId, type, resourceId, request.revision());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/api/v1/daily-notes/{dailyNoteId}/indexing")
    public ResponseEntity<Void> indexDailyNote(@PathVariable Long dailyNoteId,
                                               @AuthMemberId Long memberId) {
        service.indexDailyNote(memberId, dailyNoteId);
        return ResponseEntity.accepted().build();
    }

    @ExceptionHandler(ContentIndexingService.StaleContentException.class)
    public ResponseEntity<Void> stale() {
        return ResponseEntity.status(409).build();
    }

    public record DocumentIndexingRequest(Long revision) {
    }
}
