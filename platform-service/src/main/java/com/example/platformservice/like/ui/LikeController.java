package com.example.platformservice.like.ui;

import com.example.platformservice.like.application.LikeRequest;
import com.example.platformservice.like.application.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/api/v1/like")
@RestController
public class LikeController {

    private final LikeService likeService;

    @PostMapping
    public ResponseEntity<Long> like(
            final LikeRequest request,
            @RequestHeader("X-Member-Id") final Long memberId
    ) {
        Long likeId = likeService.like(memberId, request.getNoteInfoId());
        return ResponseEntity.ok(likeId);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteLike(
            final LikeRequest request,
            @RequestHeader("X-Member-Id") final Long memberId
    ) {
        likeService.deleteLike(memberId, request.getNoteInfoId());
        return ResponseEntity.ok().build();
    }


    @GetMapping("/{noteInfoId}")
    public ResponseEntity<Long> countLikes(
            @PathVariable("noteInfoId") final Long noteInfoId
    ) {
        Long likesCount = likeService.countLikesByNoteInfoId(noteInfoId);
        return ResponseEntity.ok(likesCount);
    }

}
