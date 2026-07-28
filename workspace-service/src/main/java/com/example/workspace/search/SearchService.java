package com.example.workspace.search;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class SearchService {

    private final SearchRepository searchRepository;

    @Transactional(readOnly = true)
    public SearchResponse searchAllNotes(final Long memberId, final String targetWord) {
        if (targetWord == null || targetWord.isBlank()) {
            return new SearchResponse(List.of());
        }

        List<SearchDocumentRow> documentRows = searchRepository.searchWorkspaceNotes(memberId, targetWord);
        List<SearchResponse.SearchResult> results = new ArrayList<>(documentRows.stream()
                .map(row -> new SearchResponse.SearchResult(
                        row.id(),
                        row.type(),
                        row.title(),
                        row.content(),
                        row.createdAt(),
                        null
                ))
                .toList());
        results.addAll(searchRepository.searchDailyNotes(memberId, targetWord));
        results.sort((left, right) -> right.createdAt().compareTo(left.createdAt()));
        return new SearchResponse(results);
    }

}
