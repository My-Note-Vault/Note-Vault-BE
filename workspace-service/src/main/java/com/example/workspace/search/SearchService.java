package com.example.workspace.search;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class SearchService {

    private static final int SNIPPET_CONTEXT_LENGTH = 5;

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
                        createSnippet(row.content(), targetWord),
                        row.createdAt(),
                        null
                ))
                .toList());
        results.addAll(searchRepository.searchDailyNotes(memberId, targetWord).stream()
                .map(result -> new SearchResponse.SearchResult(
                        result.id(),
                        result.type(),
                        result.title(),
                        createSnippet(result.content(), targetWord),
                        result.createdAt(),
                        result.logicalDate()
                ))
                .toList());
        results.sort((left, right) -> right.createdAt().compareTo(left.createdAt()));
        return new SearchResponse(results);
    }

    private String createSnippet(final String content, final String targetWord) {
        if (content == null || content.isBlank()) {
            return null;
        }

        String target = targetWord.strip();
        int targetIndex = findIgnoreCase(content, target);
        if (targetIndex < 0) {
            return null;
        }

        int targetEnd = targetIndex + target.length();
        int precedingCharacters = content.codePointCount(0, targetIndex);
        int followingCharacters = content.codePointCount(targetEnd, content.length());
        int start = content.offsetByCodePoints(
                targetIndex,
                -Math.min(SNIPPET_CONTEXT_LENGTH, precedingCharacters)
        );
        int end = content.offsetByCodePoints(
                targetEnd,
                Math.min(SNIPPET_CONTEXT_LENGTH, followingCharacters)
        );

        return (start > 0 ? "..." : "")
                + content.substring(start, end)
                + (end < content.length() ? "..." : "");
    }

    private int findIgnoreCase(final String content, final String target) {
        for (int index = 0; index <= content.length() - target.length(); index++) {
            if (content.regionMatches(true, index, target, 0, target.length())) {
                return index;
            }
        }
        return -1;
    }

}
