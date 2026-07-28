package com.example.workspace.document.command.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface DocumentDeltaRepository extends JpaRepository<DocumentDelta, Long> {

    List<DocumentDelta> findAllByDocumentIdAndRevisionGreaterThanAndRevisionLessThanEqualOrderByRevisionAsc(
            Long documentId,
            Long revision,
            Long latestRevision
    );

    List<DocumentDelta> findTop500ByDocumentIdAndRevisionGreaterThanAndRevisionLessThanEqualOrderByRevisionAsc(
            Long documentId,
            Long revision,
            Long targetRevision
    );

    @Query(
            value = """
                    select coalesce(sum(octet_length(yjs_update)), 0)
                    from document_delta
                    where document_id = :documentId
                      and revision > :fromRevision
                      and revision <= :toRevision
                    """,
            nativeQuery = true
    )
    Long sumCrdtUpdateBytes(
            @Param("documentId") Long documentId,
            @Param("fromRevision") Long fromRevision,
            @Param("toRevision") Long toRevision
    );

    Optional<DocumentDelta> findByDocumentIdAndClientUpdateId(
            Long documentId,
            String clientUpdateId
    );
}
