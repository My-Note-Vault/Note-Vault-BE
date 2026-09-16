package com.example.workspace.workspace.query.response;

import com.example.workspace.document.command.domain.DocumentType;

import java.util.List;

public record WorkspaceDocumentTreeNodeResponse(
        Long id,
        DocumentType type,
        String title,
        Long parentId,
        List<WorkspaceDocumentTreeNodeResponse> children
) {}
