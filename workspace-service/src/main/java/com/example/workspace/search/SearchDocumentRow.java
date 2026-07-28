package com.example.workspace.search;

import java.time.LocalDateTime;

record SearchDocumentRow(
        SearchDocumentType type,
        Long id,
        String title,
        String content,
        LocalDateTime createdAt
) {
}
