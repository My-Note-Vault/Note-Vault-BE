package com.example.workspace.note.command.application;

import com.example.common.file.image.ImageUtils;
import com.example.workspace.note.command.domain.Note;
import com.example.workspace.note.command.domain.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class NoteCommandService {

    private final NoteRepository noteRepository;
    private final ImageUtils imageUtils;

    @Transactional
    public Long createNote(final Long authorId, final Long subTaskId) {
        Note note = new Note(authorId, subTaskId);
        noteRepository.save(note);

        return note.getId();
    }


    @Transactional
    public void editNote(final Long memberId, final Long noteId, final Long subTaskId, final String title, final String content, final Boolean isPublic) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoSuchElementException("DailyNote 를 찾을 수 없습니다"));

        String oldContent = note.getContent();
        note.edit(memberId, subTaskId, title, content, isPublic);
        imageUtils.deleteRemovedContentImages(oldContent, content);
        noteRepository.save(note);
    }

    @Transactional
    public void deleteNote(final Long memberId, final Long noteId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoSuchElementException("DailyNote 를 찾을 수 없습니다"));

        if (!note.getAuthorId().equals(memberId)) {
            throw new IllegalArgumentException("삭제할 권한이 없습니다.");
        }
        imageUtils.deleteAllContentImages(note.getContent());
        noteRepository.delete(note);
    }
}
