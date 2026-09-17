package com.example.workspace.search;

import java.time.LocalDate;
import java.time.LocalDateTime;

record SearchDocumentRow(
        SearchDocumentType type,
        Long id,
        Long sourceId,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDate logicalDate,
        Long sourceRevision
) {
}
