package com.example.workspace.unfolded;

import com.example.workspace.unfolded.domain.UnfoldedNoteId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.common.CommonConstant.AUTHORIZED_MEMBER_ID;

@RequiredArgsConstructor
@RequestMapping("/api/v1/unfolded-notes")
@RestController
public class UnfoldedNoteController {

    private final UnfoldedNoteService unfoldedNoteService;

    @GetMapping
    public ResponseEntity<List<UnfoldedNoteId>> findAll(@RequestAttribute(AUTHORIZED_MEMBER_ID) final Long memberId) {
        List<UnfoldedNoteId> allUnfoldedNoteIds = unfoldedNoteService.findAllUnfoldedNotes(memberId);
        return ResponseEntity.ok(allUnfoldedNoteIds);
    }

    @PostMapping
    public ResponseEntity<Void> updateSidebar(
            @RequestBody final List<UnfoldedNoteId> request,
            @RequestAttribute(AUTHORIZED_MEMBER_ID) final Long memberId
    ) {
        unfoldedNoteService.replaceAll(request, memberId);
        return ResponseEntity.noContent().build();
    }
}
