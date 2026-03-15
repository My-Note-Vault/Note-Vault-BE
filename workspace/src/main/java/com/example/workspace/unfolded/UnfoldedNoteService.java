package com.example.workspace.unfolded;

import com.example.workspace.unfolded.domain.NoteType;
import com.example.workspace.unfolded.domain.UnfoldedNote;
import com.example.workspace.unfolded.domain.UnfoldedNoteId;
import com.example.workspace.unfolded.domain.UnfoldedNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class UnfoldedNoteService {

    private final UnfoldedNoteRepository unfoldedNoteRepository;

    @Transactional(readOnly = true)
    public List<UnfoldedNoteId> findAllUnfoldedNotes(final Long authorId) {
        List<UnfoldedNote> allByAuthorId = unfoldedNoteRepository.findAllByAuthorId(authorId);

        return allByAuthorId.stream()
                .map(UnfoldedNote::getId)
                .toList();
    }

    @Transactional
    public void replaceAll(final List<UnfoldedNoteId> unfoldedNoteIds, final Long authorId) {
        unfoldedNoteRepository.deleteAllByAuthorId(authorId);

        List<UnfoldedNote> unfoldedNotes = unfoldedNoteIds.stream()
                .map(id -> new UnfoldedNote(id, authorId))
                .toList();

        unfoldedNoteRepository.saveAll(unfoldedNotes);
    }
}
