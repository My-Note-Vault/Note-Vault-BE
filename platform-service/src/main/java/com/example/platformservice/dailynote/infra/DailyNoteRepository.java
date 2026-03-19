package com.example.platformservice.dailynote.infra;

import com.example.platformservice.dailynote.command.domain.DailyNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface DailyNoteRepository extends JpaRepository<DailyNote, Long>, DailyNoteJdbcRepository {

    List<DailyNote> findAllByAuthorId(Long authorId);

    List<DailyNote> findAllByCreatedAt(LocalDateTime createdAt);

}
