package com.example.platformservice.viewed.ui;

import com.example.platformservice.viewed.application.RecentlyViewedInfoService;
import com.example.platformservice.viewed.domain.RecentlyViewedId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/api/v1/recently-viewed-info")
@RestController
public class RecentlyViewedInfoController {

    private final RecentlyViewedInfoService recentlyViewedInfoService;

    @PostMapping("/{infoId}")
    public ResponseEntity<RecentlyViewedId> save(
            @PathVariable("infoId") final Long infoId,
            @RequestHeader("X-Member-Id") final Long memberId
    ) {
        RecentlyViewedId id = recentlyViewedInfoService.save(infoId, memberId);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{infoId}")
    public ResponseEntity<Void> delete(
            @PathVariable("infoId") final Long infoId,
            @RequestHeader("X-Member-Id") final Long memberId
    ) {
        recentlyViewedInfoService.delete(infoId, memberId);
        return ResponseEntity.noContent().build();
    }
}
