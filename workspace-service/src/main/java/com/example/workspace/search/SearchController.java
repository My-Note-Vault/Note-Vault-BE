package com.example.workspace.search;

import com.example.common.AuthMemberId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RequiredArgsConstructor
@RequestMapping("/api/v1/search")
@RestController
public class SearchController {

    private final SearchService searchService;

    @PostMapping
    public ResponseEntity<SearchResponse> searchAllNotes(
            @RequestBody final SearchNoteRequest request,
            @AuthMemberId final Long memberId
    ) {
        SearchResponse searchResults = searchService.searchAllNotes(memberId, request.targetWord());
        return ResponseEntity.ok(searchResults);
    }
}
