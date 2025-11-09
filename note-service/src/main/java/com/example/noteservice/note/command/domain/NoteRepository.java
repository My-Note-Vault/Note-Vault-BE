package com.example.noteservice.note.command.domain;

import com.example.noteservice.note.infra.NoteDslRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<Note, Long>, NoteDslRepository {

}
