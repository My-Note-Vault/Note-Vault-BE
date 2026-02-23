package com.example.workspace.note.query;

import com.example.workspace.note.command.domain.Note;
import com.example.workspace.note.command.domain.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class NoteQueryService {

    private final NoteRepository noteRepository;

    public Note findNoteById(final Long noteId) {
        return noteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("노트가 존재하지 않습니다!"));
    }




}
