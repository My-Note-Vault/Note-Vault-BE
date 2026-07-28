package com.example.workspace.document.ui.request;

import com.example.workspace.task.command.domain.value.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class EditDocumentRequest {

    private final String title;

    private final Status status;
    private final LocalDateTime startDateTime;
    private final LocalDateTime endDateTime;
    private final Boolean isPublic;
    private final Long parentId;
}
