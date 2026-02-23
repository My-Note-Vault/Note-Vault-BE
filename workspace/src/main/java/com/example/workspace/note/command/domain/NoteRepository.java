package com.example.workspace.note.command.domain;

import com.example.workspace.note.infra.NoteDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<Note, Long>, NoteDslRepository {

}
