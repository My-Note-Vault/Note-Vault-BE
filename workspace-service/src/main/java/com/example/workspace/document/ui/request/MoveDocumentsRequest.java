package com.example.workspace.document.ui.request;

import java.util.List;

public record MoveDocumentsRequest(Long workSpaceId, List<Long> documentIds, Long parentId) {
}
