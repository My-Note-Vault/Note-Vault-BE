package com.example.noteservice.snapshot.command.domain;

import com.example.noteservice.snapshot.infra.SnapshotDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SnapshotRepository extends JpaRepository<Snapshot, Long>, SnapshotDslRepository {
    List<Snapshot> findByNoteId(Long noteId);

    Optional<Snapshot> findSnapshotById(Long id);
}
