package com.example.workspace.search;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.example.common.CommonConstant.AUTHORIZED_MEMBER_ID;

@RequiredArgsConstructor
@RequestMapping("/api/v1/search")
@RestController
public class SearchController {

    private final SearchService searchService;

    @PostMapping
    public ResponseEntity<NoteGroup> searchAllNotes(
            @RequestBody final SearchNoteRequest request,
            @RequestAttribute(AUTHORIZED_MEMBER_ID) final Long memberId
    ) {
        NoteGroup searchResults = searchService.searchAllNotes(memberId, request.targetWord());
        return ResponseEntity.ok(searchResults);
    }
}
