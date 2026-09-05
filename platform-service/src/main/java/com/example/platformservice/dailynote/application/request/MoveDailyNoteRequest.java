package com.example.platformservice.dailynote.application.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MoveDailyNoteRequest {
    private final Long folderId;
}
