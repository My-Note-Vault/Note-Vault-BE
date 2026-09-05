package com.example.workspace.document.command.application;

import com.example.common.exception.ForbiddenException;
import com.example.common.file.image.ImageUtils;
import com.example.workspace.document.command.domain.Document;
import com.example.workspace.document.command.domain.DocumentDelta;
import com.example.workspace.document.command.domain.DocumentDeltaRepository;
import com.example.workspace.document.command.domain.DocumentEditActivity;
import com.example.workspace.document.command.domain.DocumentEditActivityRepository;
import com.example.workspace.document.command.domain.DocumentRepository;
import com.example.workspace.document.command.domain.DocumentType;
import com.example.workspace.task.command.domain.value.Status;
import com.example.workspace.workspace.command.domain.ParticipantRepository;
import com.example.workspace.workspace.command.domain.WorkSpace;
import com.example.workspace.workspace.command.domain.WorkSpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class DocumentCommandService {

    private static final ZoneId ACTIVITY_ZONE = ZoneId.of("Asia/Seoul");

    private final DocumentRepository documentRepository;
    private final DocumentDeltaRepository documentDeltaRepository;
    private final DocumentEditActivityRepository documentEditActivityRepository;
    private final ParticipantRepository participantRepository;
    private final WorkSpaceRepository workSpaceRepository;
    private final ImageUtils imageUtils;

    @Transactional
    public Long createDocument(
            final Long memberId,
            final DocumentType type,
            final Long workSpaceId,
            final Long parentId
    ) {
        if (type == DocumentType.WORKSPACE_HOME) {
            throw new IllegalArgumentException("Workspace home 문서는 직접 생성할 수 없습니다");
        }

        Document parent = parentId == null ? null : findParent(parentId, type);
        Long resolvedWorkSpaceId = parent == null ? workSpaceId : parent.getWorkSpaceId();
        if (resolvedWorkSpaceId == null) {
            throw new IllegalArgumentException("Workspace 를 지정해야 합니다");
        }
        if (parent != null && workSpaceId != null && !workSpaceId.equals(resolvedWorkSpaceId)) {
            throw new IllegalArgumentException("부모 문서와 같은 Workspace 에만 생성할 수 있습니다");
        }
        validateParticipant(resolvedWorkSpaceId, memberId, type);

        Document document = type == DocumentType.TASK
                ? Document.task(resolvedWorkSpaceId, parentId, memberId)
                : Document.note(resolvedWorkSpaceId, parentId, memberId);
        documentRepository.save(document);
        return document.getId();
    }

    @Transactional
    public Long ensureWorkspaceHomeDocument(final Long memberId, final Long workSpaceId) {
        validateParticipant(workSpaceId, memberId, DocumentType.WORKSPACE_HOME);
        WorkSpace workSpace = workSpaceRepository.findWithWriteLockById(workSpaceId)
                .orElseThrow(() -> new NoSuchElementException("WorkSpace 를 찾을 수 없습니다"));

        return documentRepository
                .findByWorkSpaceIdAndType(workSpaceId, DocumentType.WORKSPACE_HOME)
                .map(Document::getId)
                .orElseGet(() -> documentRepository.save(
                        Document.workspaceHome(
                                workSpaceId,
                                workSpace.getCreatorId(),
                                workSpace.getContent()
                        )
                ).getId());
    }

    @Transactional
    public void editDocument(
            final Long memberId,
            final Long documentId,
            final DocumentType type,
            final Long parentId,
            final String title,
            final Status status,
            final LocalDateTime startDateTime,
            final LocalDateTime endDateTime,
            final Boolean isPublic
    ) {
        Document document = findByIdAndType(documentId, type);
        validateParticipant(document.getWorkSpaceId(), memberId, type);

        if (parentId != null) {
            Document parent = findParent(parentId, type);
            validateParticipant(parent.getWorkSpaceId(), memberId, type);
            validateMove(document, parent);
            document.moveTo(parent);
        }

        document.edit(title, null, null, status, startDateTime, endDateTime, isPublic);

        documentRepository.save(document);
    }

    @Transactional
    public void initializeDocument(
            final Long memberId,
            final Long documentId,
            final DocumentType type,
            final String title,
            final Status status,
            final LocalDateTime startDateTime,
            final LocalDateTime endDateTime,
            final Boolean isPublic
    ) {
        Document document = findByIdAndType(documentId, type);
        validateParticipant(document.getWorkSpaceId(), memberId, type);
        document.edit(
                title,
                null,
                null,
                status,
                startDateTime,
                endDateTime,
                isPublic
        );
        documentRepository.save(document);
    }

    @Transactional
    public void deleteDocument(final Long memberId, final Long documentId, final DocumentType type) {
        Document document = findByIdAndType(documentId, type);
        validateParticipant(document.getWorkSpaceId(), memberId, type);

        if (!document.getAuthorId().equals(memberId)) {
            throw new ForbiddenException(type.deleteDeniedMessage());
        }

        imageUtils.deleteAllContentImages(document.getSearchContent());
        documentRepository.findAllByParentId(documentId).forEach(child -> {
            child.reparentTo(document.getParentId());
            documentRepository.save(child);
        });
        documentRepository.delete(document);
    }

    @Transactional
    public CommittedCrdtUpdate appendDocumentDelta(
            final Long memberId,
            final Long workSpaceId,
            final Long documentId,
            final DocumentType type,
            final String clientUpdateId,
            final int insertedCharacterCount,
            final byte[] crdtUpdate
    ) {
        Document document = documentRepository.findWithLockByIdAndType(documentId, type)
                .orElseThrow(() -> new NoSuchElementException(type.notFoundMessage()));
        validateParticipant(document.getWorkSpaceId(), memberId, type);
        if (!document.getWorkSpaceId().equals(workSpaceId)) {
            throw new NoSuchElementException(type.notFoundMessage());
        }

        return documentDeltaRepository.findByDocumentIdAndClientUpdateId(documentId, clientUpdateId)
                .map(delta -> new CommittedCrdtUpdate(
                        delta.getRevision(),
                        delta.getClientUpdateId(),
                        delta.getCrdtUpdate()
                ))
                .orElseGet(() -> {
                    long revision = document.issueNextRevision();
                    DocumentDelta delta = documentDeltaRepository.save(
                            new DocumentDelta(document, revision, clientUpdateId, crdtUpdate)
                    );
                    if (insertedCharacterCount > 0) {
                        documentEditActivityRepository.save(new DocumentEditActivity(
                                documentId, memberId, clientUpdateId,
                                insertedCharacterCount, LocalDate.now(ACTIVITY_ZONE)
                        ));
                    }
                    documentRepository.save(document);
                    return new CommittedCrdtUpdate(
                            delta.getRevision(),
                            delta.getClientUpdateId(),
                            delta.getCrdtUpdate()
                    );
                });
    }

    @Transactional
    public boolean updateSearchProjection(
            final Long memberId,
            final Long workSpaceId,
            final Long documentId,
            final DocumentType type,
            final Long revision,
            final String searchContent,
            final byte[] crdtState
    ) {
        Document document = documentRepository.findWithLockByIdAndType(documentId, type)
                .orElseThrow(() -> new NoSuchElementException(type.notFoundMessage()));
        validateParticipant(document.getWorkSpaceId(), memberId, type);
        if (!document.getWorkSpaceId().equals(workSpaceId)) {
            throw new NoSuchElementException(type.notFoundMessage());
        }

        String oldSearchContent = document.getSearchContent();
        boolean updated = document.updateSearchProjection(searchContent, crdtState, revision);
        if (updated) {
            imageUtils.deleteRemovedContentImages(oldSearchContent, searchContent);
            documentRepository.save(document);
        }
        return updated;
    }

    private Document findByIdAndType(final Long id, final DocumentType type) {
        return documentRepository.findByIdAndType(id, type)
                .orElseThrow(() -> new NoSuchElementException(type.notFoundMessage()));
    }

    private Document findParent(final Long parentId, final DocumentType type) {
        if (parentId == null) {
            throw new NoSuchElementException(type.parentNotFoundMessage());
        }
        return documentRepository.findById(parentId)
                .filter(parent -> parent.getType() != DocumentType.WORKSPACE_HOME)
                .orElseThrow(() -> new NoSuchElementException(type.parentNotFoundMessage()));
    }

    private void validateMove(final Document document, final Document newParent) {
        if (!document.getWorkSpaceId().equals(newParent.getWorkSpaceId())) {
            throw new IllegalArgumentException("다른 Workspace 로 문서를 이동할 수 없습니다");
        }

        Document current = newParent;
        while (current != null) {
            if (current.getId().equals(document.getId())) {
                throw new IllegalArgumentException("문서를 자기 자신이나 하위 문서 아래로 이동할 수 없습니다");
            }
            current = current.getParentId() == null
                    ? null
                    : documentRepository.findById(current.getParentId()).orElse(null);
        }
    }

    private void validateParticipant(final Long workSpaceId, final Long memberId, final DocumentType type) {
        participantRepository.findByWorkSpaceIdAndMemberId(workSpaceId, memberId)
                .orElseThrow(() -> new ForbiddenException(type.accessDeniedMessage()));
    }

    public record CommittedCrdtUpdate(
            Long revision,
            String clientUpdateId,
            byte[] crdtUpdate
    ) {
    }
}
