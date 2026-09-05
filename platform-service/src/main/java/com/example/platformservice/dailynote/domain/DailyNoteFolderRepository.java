package com.example.platformservice.dailynote.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DailyNoteFolderRepository extends JpaRepository<DailyNoteFolder, Long> {

    List<DailyNoteFolder> findAllByAuthorIdOrderByNameAsc(Long authorId);

    Optional<DailyNoteFolder> findByIdAndAuthorId(Long id, Long authorId);

    boolean existsByAuthorIdAndName(Long authorId, String name);

    boolean existsByAuthorIdAndNameAndIdNot(Long authorId, String name, Long id);
}
