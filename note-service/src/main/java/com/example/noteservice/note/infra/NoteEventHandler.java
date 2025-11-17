package com.example.noteservice.note.infra;

import com.example.noteservice.note.command.domain.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class NoteEventHandler {

    private final NoteRepository noteRepository;
}
