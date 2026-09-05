package com.example.platformservice.draw.ui;

import com.example.common.AuthMemberId;
import com.example.platformservice.draw.application.DailyDrawService;
import com.example.platformservice.draw.application.DrawOverviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/api/v1/draws")
@RestController
public class DailyDrawController {
    private final DailyDrawService service;
    @GetMapping
    public ResponseEntity<DrawOverviewResponse> overview(@AuthMemberId Long memberId) {
        return ResponseEntity.ok(service.overview(memberId));
    }
}
