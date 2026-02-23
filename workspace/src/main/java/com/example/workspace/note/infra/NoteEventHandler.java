package com.example.workspace.note.infra;

import com.example.workspace.note.command.domain.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class NoteEventHandler {

    private final NoteRepository noteRepository;
}
