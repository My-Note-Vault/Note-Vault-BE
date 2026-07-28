package com.example.workspace.document.command.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByIdAndType(Long id, DocumentType type);

    Optional<Document> findByWorkSpaceIdAndType(Long workSpaceId, DocumentType type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Document d where d.id = :id and d.type = :type")
    Optional<Document> findWithLockByIdAndType(
            @Param("id") Long id,
            @Param("type") DocumentType type
    );

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select d from Document d where d.id = :id and d.type = :type")
    Optional<Document> findWithReadLockByIdAndType(
            @Param("id") Long id,
            @Param("type") DocumentType type
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Document d where d.id = :id")
    Optional<Document> findWithWriteLockById(@Param("id") Long id);

    @Query("""
            select d.id
            from Document d
            where d.crdtState is not null
              and d.searchRevision > coalesce(d.compactedRevision, 0)
            """)
    List<Long> findCrdtCompactionCandidateIds();

    Optional<Document> findByIdAndTypeAndAuthorId(Long id, DocumentType type, Long authorId);

    List<Document> findAllByAuthorId(Long authorId);

    List<Document> findAllByAuthorIdAndType(Long authorId, DocumentType type);

    List<Document> findAllByWorkSpaceIdAndType(Long workSpaceId, DocumentType type);
}
