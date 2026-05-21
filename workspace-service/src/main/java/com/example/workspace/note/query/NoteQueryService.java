package com.example.workspace.note.query;

import com.example.workspace.note.command.domain.Note;
import com.example.workspace.note.command.domain.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class NoteQueryService {

    private final NoteRepository noteRepository;

    public Note findNoteById(
            final Long authorId,
            final Long noteId
    ) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoSuchElementException("DailyNote 를 찾을 수 없습니다"));

        return note;
    }

    public List<Note> findAllNoteByAuthorId(final Long authorId) {
        return noteRepository.findAllByAuthorId(authorId);
    }


}
