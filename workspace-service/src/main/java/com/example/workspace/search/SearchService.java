package com.example.workspace.search;

import com.notevault.workspace.api.search.KeywordSearchHit;
import com.notevault.workspace.api.search.KeywordSearchReader;
import com.notevault.workspace.api.search.KeywordSourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class SearchService implements KeywordSearchReader {

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

    @Override
    @Transactional(readOnly = true)
    public List<KeywordSearchHit> search(
            final Long memberId,
            final List<String> keywords,
            final int limitPerKeyword
    ) {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(keywords, "keywords");
        if (limitPerKeyword < 0) {
            throw new IllegalArgumentException("limitPerKeyword must be non-negative");
        }
        if (keywords.isEmpty() || limitPerKeyword == 0) {
            return List.of();
        }

        Map<SourceKey, KeywordSearchHit> hits = new LinkedHashMap<>();
        keywords.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(keyword -> !keyword.isEmpty())
                .distinct()
                .forEach(keyword -> searchKeyword(memberId, keyword).stream()
                        .sorted(Comparator.comparingDouble(KeywordSearchHit::score).reversed())
                        .limit(limitPerKeyword)
                        .forEach(hit -> hits.merge(
                                new SourceKey(hit.sourceType(), hit.sourceId()),
                                hit,
                                (left, right) -> left.score() >= right.score() ? left : right
                        )));
        return hits.values().stream()
                .sorted(Comparator.comparingDouble(KeywordSearchHit::score).reversed())
                .toList();
    }

    private List<KeywordSearchHit> searchKeyword(final Long memberId, final String keyword) {
        List<SearchDocumentRow> rows = new ArrayList<>(searchRepository.searchWorkspaceNotes(memberId, keyword));
        rows.addAll(searchRepository.searchDailyNotes(memberId, keyword));
        // Preserve the existing newest-first tie break before ranking by keyword score.
        rows.sort(Comparator.comparing(SearchDocumentRow::createdAt).reversed());

        List<KeywordSearchHit> hits = new ArrayList<>();
        for (SearchDocumentRow row : rows) {
            // A legacy workspace without a home document has no DOCUMENT source to join to.
            if (row.sourceId() == null) {
                continue;
            }
            String snippet = createSnippet(row.content(), keyword);
            boolean titleMatched = row.title() != null
                    && row.title().toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
            double score = titleMatched ? 1.0 : (snippet != null ? 0.7 : 0.0);
            if (score <= 0) {
                continue;
            }
            KeywordSourceType sourceType = row.type() == SearchDocumentType.DAILY_NOTE
                    ? KeywordSourceType.DAILY_NOTE : KeywordSourceType.DOCUMENT;
            String resourceType = switch (row.type()) {
                case WORKSPACE -> "space";
                case DAILY_NOTE -> "daily";
                case TASK -> "task";
                case NOTE -> "note";
            };
            hits.add(new KeywordSearchHit(
                    sourceType, row.sourceId(), resourceType, row.id(),
                    row.title(), snippet, row.sourceRevision(), score
            ));
        }
        return hits;
    }

    private record SourceKey(KeywordSourceType sourceType, Long sourceId) {
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
