package com.example.workspace.search;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SearchResponse(
        List<SearchResult> results
) {

    public record SearchResult(
            Long id,
            SearchDocumentType type,
            String title,
            String content,
            LocalDateTime createdAt,
            LocalDate logicalDate
    ) {
    }
}
