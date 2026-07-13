package com.example.workspace.document.command.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByIdAndType(Long id, DocumentType type);

    Optional<Document> findByIdAndTypeAndAuthorId(Long id, DocumentType type, Long authorId);

    List<Document> findAllByAuthorId(Long authorId);

    List<Document> findAllByAuthorIdAndType(Long authorId, DocumentType type);

    List<Document> findAllByWorkSpaceIdAndType(Long workSpaceId, DocumentType type);
}
