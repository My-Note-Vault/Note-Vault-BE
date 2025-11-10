package com.example.noteservice.note.command.application;

import com.example.noteservice.note.command.application.request.EditNoteRequest;
import com.example.noteservice.note.command.application.request.NoteDelta;
import com.example.noteservice.note.command.domain.Note;
import com.example.noteservice.note.command.domain.NoteRepository;
import com.example.noteservice.note.query.response.EditNoteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class NoteCommandService {

    private final NoteRepository noteRepository;

    @Transactional
    public Long createNote(final Long memberId) {
        Note note = new Note(memberId);
        noteRepository.save(note);

        return note.getId();
    }


    @Transactional
    public EditNoteResponse editNote(final EditNoteRequest request) {
        Note note = noteRepository.findById(request.getNoteId())
                .orElseThrow(() -> new NoSuchElementException("일치하는 Note 가 없습니다"));

        if (!note.getVersion().equals(request.getBaseVersion())) {
            throw new NoSuchElementException("일치하는 Note 가 없습니다");
        }

        String updated = applyDeltaOperation(note.getContent(), request.getDeltas());
        note.edit(request.getAuthorId(), updated);

        return new EditNoteResponse(
                note.getId(),
                note.getVersion(),
                note.getUpdatedAt()
        );
    }


    @Transactional
    public void deleteNote(final Long noteId, final Long memberId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoSuchElementException("해당하는 노트가 없습니다"));

        if (!note.getAuthorId().equals(memberId)) {
            throw new NoSuchElementException("작성자만 삭제할 수 있습니다");
        }
        noteRepository.delete(note);
    }

    private String applyDeltaOperation(
            final String content,
            List<NoteDelta> deltas
    ) {
        if (deltas == null || deltas.isEmpty()) {
            return content;
        }
        deltas.sort((a, b) -> Integer.compare(maxIndex(b), maxIndex(a)));

        StringBuilder stringBuilder = new StringBuilder(content);
        for (NoteDelta delta : deltas) {
            switch (delta.getOperation()) {
                case "insert" -> {
                    int pos = reqIndex(delta.getPos(), stringBuilder.length());
                    stringBuilder.insert(pos, nullToEmpty(delta.getText()));
                }
                case "delete" -> {
                    int start = reqIndex(delta.getStart(), stringBuilder.length());
                    int end = reqIndex(delta.getEnd(), stringBuilder.length());
                    bounds(start, end, stringBuilder.length());
                    stringBuilder.delete(start, end);
                }
                case "replace" -> {
                    int start = reqIndex(delta.getStart(), stringBuilder.length());
                    int end = reqIndex(delta.getEnd(), stringBuilder.length());
                    bounds(start, end, stringBuilder.length());
                    stringBuilder.replace(start, end, nullToEmpty(delta.getText()));
                }
                default -> throw new NoSuchElementException("올바르지 않은 연산코드입니다");
            }
        }
        return stringBuilder.toString();
    }

    private int maxIndex(final NoteDelta delta) {
        if ("insert".equals(delta.getOperation())) {
            return safe(delta.getPos());
        }
        return Math.max(safe(delta.getStart()), safe(delta.getEnd()));
    }


    private int safe(Integer v) {
        return v == null ? 0 : v;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private int reqIndex(Integer v, int len) {
        if (v == null) throw new IllegalArgumentException("index required");
        if (v < 0 || v > len) throw new IllegalArgumentException("index out of range: " + v);
        return v;
    }

    private void bounds(int start, int end, int len) {
        if (start < 0 || end < 0 || start > end || end > len)
            throw new IllegalArgumentException("range out of bounds: " + start + "~" + end + " / len=" + len);
    }

}
