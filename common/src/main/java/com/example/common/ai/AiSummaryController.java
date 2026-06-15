package com.example.common.ai;

import com.example.common.AuthMemberId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/v1/ai")
@RestController
public class AiSummaryController {

    private final AiSummaryService aiSummaryService;

    @PostMapping("/summaries")
    public ResponseEntity<AiSummaryResponse> summarize(
            @RequestBody final AiSummaryRequest request,
            @AuthMemberId final Long memberId
    ) {
        return ResponseEntity.ok(aiSummaryService.summarize(request));
    }
}
