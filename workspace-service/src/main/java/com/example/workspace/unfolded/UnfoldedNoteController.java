package com.example.workspace.unfolded;

import com.example.common.AuthMemberId;
import com.example.workspace.unfolded.domain.UnfoldedNoteId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RequiredArgsConstructor
@RequestMapping("/api/v1/unfolded-notes")
@RestController
public class UnfoldedNoteController {

    private final UnfoldedNoteService unfoldedNoteService;

    @GetMapping
    public ResponseEntity<List<UnfoldedNoteId>> findAll(@AuthMemberId final Long memberId) {
        List<UnfoldedNoteId> allUnfoldedNoteIds = unfoldedNoteService.findAllUnfoldedNotes(memberId);
        return ResponseEntity.ok(allUnfoldedNoteIds);
    }

    @GetMapping("/note-info")
    public ResponseEntity<List<TaskOverviewResponse>> findAllNotesInfo(
            @RequestParam("workspace") Long workSpaceId,
            @AuthMemberId final Long memberId
    ) {
        List<TaskOverviewResponse> allNotesInfo = unfoldedNoteService.findAllNotesInfo(memberId, workSpaceId);
        return ResponseEntity.ok(allNotesInfo);
    }

    @PostMapping
    public ResponseEntity<Void> addSidebarNote(
            @RequestBody final UnfoldedNoteId request,
            @AuthMemberId final Long memberId
    ) {
        unfoldedNoteService.addSidebarInfo(request, memberId);
        return ResponseEntity.noContent().build();
    }
}
