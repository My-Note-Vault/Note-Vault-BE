package com.example.workspace.unfolded.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnfoldedNoteRepository extends JpaRepository<UnfoldedNote, UnfoldedNoteId>, UnfoldedNoteJdbcRepository {
    List<UnfoldedNote> findByAuthorId(Long authorId);

    List<UnfoldedNote> findAllByAuthorId(Long authorId);

    void deleteAllByAuthorId(Long authorId);
}
