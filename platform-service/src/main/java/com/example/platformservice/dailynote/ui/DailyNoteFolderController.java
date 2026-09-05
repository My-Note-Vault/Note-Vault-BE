package com.example.platformservice.dailynote.ui;

import com.example.common.AuthMemberId;
import com.example.platformservice.dailynote.application.DailyNoteFolderService;
import com.example.platformservice.dailynote.application.request.CreateDailyNoteFolderRequest;
import com.example.platformservice.dailynote.application.request.RenameDailyNoteFolderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/api/v1/daily-note-folders")
@RestController
public class DailyNoteFolderController {

    private final DailyNoteFolderService folderService;

    @PostMapping
    public ResponseEntity<Long> create(
            @RequestBody final CreateDailyNoteFolderRequest request,
            @AuthMemberId final Long memberId
    ) {
        return ResponseEntity.ok(folderService.create(memberId, request.getName()));
    }

    @PatchMapping("/{folderId}")
    public ResponseEntity<Void> rename(
            @PathVariable final Long folderId,
            @RequestBody final RenameDailyNoteFolderRequest request,
            @AuthMemberId final Long memberId
    ) {
        folderService.rename(memberId, folderId, request.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{folderId}")
    public ResponseEntity<Void> delete(
            @PathVariable final Long folderId,
            @AuthMemberId final Long memberId
    ) {
        folderService.delete(memberId, folderId);
        return ResponseEntity.noContent().build();
    }
}
