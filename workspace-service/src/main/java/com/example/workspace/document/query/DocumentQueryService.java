package com.example.workspace.document.query;

import com.example.common.exception.ForbiddenException;
import com.example.workspace.document.command.domain.DocumentType;
import com.example.workspace.document.command.domain.Document;
import com.example.workspace.document.command.domain.DocumentDelta;
import com.example.workspace.document.command.domain.DocumentDeltaRepository;
import com.example.workspace.document.command.domain.DocumentRepository;
import com.example.workspace.workspace.command.domain.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class DocumentQueryService {

    private final DocumentRepository documentRepository;
    private final DocumentDeltaRepository documentDeltaRepository;
    private final ParticipantRepository participantRepository;

    public Document findDocumentById(final Long memberId, final Long documentId, final DocumentType type) {
        Document document = documentRepository.findByIdAndType(documentId, type)
                .orElseThrow(() -> new NoSuchElementException(type.notFoundMessage()));
        validateParticipant(document.getWorkSpaceId(), memberId, type);
        return document;
    }

    public List<Document> findAllByAuthorIdAndType(final Long authorId, final DocumentType type) {
        return documentRepository.findAllByAuthorIdAndType(authorId, type);
    }

    @Transactional
    public CollaborationHistory findCollaborationBootstrap(
            final Long memberId,
            final Long workSpaceId,
            final Long documentId,
            final DocumentType type
    ) {
        Document document = type == DocumentType.WORKSPACE_HOME
                ? documentRepository.findByWorkSpaceIdAndType(workSpaceId, type)
                        .orElseThrow(() -> new NoSuchElementException(type.notFoundMessage()))
                : documentRepository.findByIdAndType(documentId, type)
                        .orElseThrow(() -> new NoSuchElementException(type.notFoundMessage()));

        return findCollaborationHistory(
                memberId,
                workSpaceId,
                document.getId(),
                type,
                0L
        );
    }

    // PostgreSQL의 SELECT ... FOR SHARE는 read-only 트랜잭션에서 실행할 수 없다.
    @Transactional
    public CollaborationHistory findCollaborationHistory(
            final Long memberId,
            final Long workSpaceId,
            final Long documentId,
            final DocumentType type
    ) {
        return findCollaborationHistory(memberId, workSpaceId, documentId, type, 0L);
    }

    @Transactional
    public CollaborationHistory findCollaborationHistory(
            final Long memberId,
            final Long workSpaceId,
            final Long documentId,
            final DocumentType type,
            final Long lastAppliedRevision
    ) {
        Document document = documentRepository.findWithReadLockByIdAndType(documentId, type)
                .orElseThrow(() -> new NoSuchElementException(type.notFoundMessage()));
        validateParticipant(document.getWorkSpaceId(), memberId, type);
        if (!document.getWorkSpaceId().equals(workSpaceId)) {
            throw new NoSuchElementException(type.notFoundMessage());
        }

        long revision = lastAppliedRevision == null ? 0L : Math.max(0L, lastAppliedRevision);
        long searchRevision = document.getSearchRevision() == null
                ? 0L
                : document.getSearchRevision();
        long snapshotRevision = 0L;
        byte[] crdtState = null;
        if (
                document.getCrdtState() != null &&
                document.getCrdtState().length > 0 &&
                revision < searchRevision
        ) {
            snapshotRevision = searchRevision;
            revision = snapshotRevision;
            crdtState = document.getCrdtState();
        }
        long latestRevision = document.getLatestRevision() == null ? 0L : document.getLatestRevision();
        List<DocumentDelta> deltas =
                documentDeltaRepository
                        .findAllByDocumentIdAndRevisionGreaterThanAndRevisionLessThanEqualOrderByRevisionAsc(
                        documentId,
                        revision,
                        latestRevision
                );
        validateContinuousRevisions(revision, latestRevision, deltas);
        return new CollaborationHistory(crdtState, snapshotRevision, deltas, latestRevision);
    }

    private void validateContinuousRevisions(
            final long baseRevision,
            final long latestRevision,
            final List<DocumentDelta> deltas
    ) {
        long expectedRevision = baseRevision + 1;
        for (DocumentDelta delta : deltas) {
            if (delta.getRevision() != expectedRevision) {
                throw new IllegalStateException(
                        "CRDT update revision gap: expected=" + expectedRevision
                                + ", actual=" + delta.getRevision()
                );
            }
            expectedRevision++;
        }
        if (expectedRevision - 1 != latestRevision) {
            throw new IllegalStateException(
                    "CRDT update history does not reach latest revision: expected="
                            + latestRevision + ", actual=" + (expectedRevision - 1)
            );
        }
    }

    private void validateParticipant(final Long workSpaceId, final Long memberId, final DocumentType type) {
        participantRepository.findByWorkSpaceIdAndMemberId(workSpaceId, memberId)
                .orElseThrow(() -> new ForbiddenException(type.accessDeniedMessage()));
    }

    public record CollaborationHistory(
            byte[] crdtState,
            long snapshotRevision,
            List<DocumentDelta> deltas,
            long latestRevision
    ) {
    }
}
