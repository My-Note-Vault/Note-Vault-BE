package com.example.noteservice.note.query;

import com.example.noteservice.note.command.domain.Note;
import com.example.noteservice.note.command.domain.NoteRepository;
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
