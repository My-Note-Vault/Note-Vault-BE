package com.example.workspace.document.command.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface DocumentDeltaArchiveRepository
        extends JpaRepository<DocumentDeltaArchive, Long> {

    @Modifying
    @Query("""
            delete from DocumentDeltaArchive archive
            where archive.deleteAfter <= :now
            """)
    int deleteExpired(@Param("now") LocalDateTime now);
}
