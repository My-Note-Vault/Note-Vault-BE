package com.example.platformservice.dailynote.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface DailyNoteFolderRepository extends JpaRepository<DailyNoteFolder, Long> {

    List<DailyNoteFolder> findAllByAuthorIdOrderByNameAsc(Long authorId);

    Optional<DailyNoteFolder> findByIdAndAuthorId(Long id, Long authorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from DailyNoteFolder f where f.id = :id and f.authorId = :authorId")
    Optional<DailyNoteFolder> findWithWriteLockByIdAndAuthorId(@Param("id") Long id, @Param("authorId") Long authorId);

    boolean existsByAuthorIdAndName(Long authorId, String name);

    boolean existsByAuthorIdAndNameAndIdNot(Long authorId, String name, Long id);
}
