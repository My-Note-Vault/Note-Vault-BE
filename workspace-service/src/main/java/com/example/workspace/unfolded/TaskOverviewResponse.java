package com.example.workspace.unfolded;

import com.example.workspace.document.command.domain.DocumentType;
import java.util.List;

public record TaskOverviewResponse(
        Long id,
        DocumentType type,
        String title,
        Long parentId,
        List<TaskOverviewResponse> children
) {}
