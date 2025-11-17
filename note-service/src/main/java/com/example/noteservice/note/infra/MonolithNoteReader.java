package com.example.noteservice.note.infra;

import com.example.common.api.NoteReader;
import com.example.common.api.NoteSummaryResponse;
import com.example.noteservice.note.command.domain.Note;
import com.example.noteservice.note.command.domain.NoteRepository;
import com.example.common.api.SnapshotDetailResponse;
import com.example.noteservice.snapshot.command.domain.Snapshot;
import com.example.noteservice.snapshot.command.domain.SnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Component
public class MonolithNoteReader implements NoteReader {

    private final NoteRepository noteRepository;
    private final SnapshotRepository snapshotRepository;

    @Transactional
    @Override
    public NoteSummaryResponse getNoteSummary(final Long noteId, final Long memberId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoSuchElementException("Note not found"));

        if (!note.getAuthorId().equals(memberId)) {
            throw new IllegalArgumentException("작성자만 접근할 수 있습니다");
        }
        return new NoteSummaryResponse(note.getTitle(), note.getContent());
    }

    @Override
    public SnapshotDetailResponse getSnapshotDetail(final Long snapshotId) {

        Snapshot snapshot = snapshotRepository.findSnapshotById(snapshotId)
                .orElseThrow(() -> new NoSuchElementException("Snapshot not found"));

        return SnapshotDetailResponse.builder()
                .authorId(snapshot.getAuthorId())
                .title(snapshot.getTitle())
                .content(snapshot.getContent())
                .previewImageKey(snapshot.getPreviewImageKey())
                .blurredImageKey(snapshot.getBlurredImageKey())
                .createdAt(snapshot.getCreatedAt())
                .build();
    }
}
