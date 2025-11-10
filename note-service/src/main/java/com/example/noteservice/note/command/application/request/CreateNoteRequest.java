package com.example.noteservice.note.command.application.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateNoteRequest {

    private final Long authorId;
}
