package com.example.workspace.unfolded.domain;

import com.example.workspace.unfolded.NoteInfoResponse;

import java.util.List;

public interface UnfoldedNoteJdbcRepository {
    void findAllUnfoldedNotes(Long authorId);

    List<NoteInfoResponse> findAllNotesInfo(Long authorId);
}
