package com.example.platformservice.dailynote.command.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DailyNoteRepository extends JpaRepository<DailyNote, Long> {

    List<DailyNote> findAllByAuthorId(Long authorId);
}
