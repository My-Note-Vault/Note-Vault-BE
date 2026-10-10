package com.example.workspace.document.command.application;

import com.example.common.exception.ForbiddenException;
import com.example.common.file.image.ImageUtils;
import com.example.search.sync.SearchSyncRecorder;
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
import java.util.Objects;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final SearchSyncRecorder searchSyncRecorder;
    private final DocumentRefreshRequests refreshRequests;

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

        // Workspace deletion holds the same lock while collecting and deleting its documents.
        workSpaceRepository.findWithWriteLockById(resolvedWorkSpaceId)
                .orElseThrow(() -> new NoSuchElementException("WorkSpace 를 찾을 수 없습니다"));

        Document document = type == DocumentType.TASK
                ? Document.task(resolvedWorkSpaceId, parentId, memberId)
                : Document.note(resolvedWorkSpaceId, parentId, memberId);
        documentRepository.save(document);
        recordRefresh(document);
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
                .orElseGet(() -> {
                    Document home = documentRepository.save(Document.workspaceHome(
                            workSpaceId, workSpace.getCreatorId(), workSpace.getContent()));
                    recordRefresh(home);
                    return home.getId();
                });
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
        Document document = findForUpdate(documentId, type);
        validateParticipant(document.getWorkSpaceId(), memberId, type);

        if (parentId != null) {
            Document parent = findParent(parentId, type);
            validateParticipant(parent.getWorkSpaceId(), memberId, type);
            validateMove(document, parent);
            document.moveTo(parent);
        }

        String oldTitle = document.getTitle();
        document.edit(title, null, null, status, startDateTime, endDateTime, isPublic);

        documentRepository.save(document);
        if (!Objects.equals(oldTitle, document.getTitle())) {
            recordRefresh(document);
        }
    }

    @Transactional
    public void moveDocuments(
            final Long memberId,
            final Long workSpaceId,
            final List<Long> documentIds,
            final Long parentId
    ) {
        if (workSpaceId == null || documentIds == null || documentIds.isEmpty()
                || documentIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("이동할 문서와 Workspace 를 지정해야 합니다");
        }
        validateParticipant(workSpaceId, memberId, DocumentType.NOTE);
        workSpaceRepository.findWithWriteLockById(workSpaceId)
                .orElseThrow(() -> new NoSuchElementException("WorkSpace 를 찾을 수 없습니다"));

        // Lock the hierarchy so concurrent moves cannot create a cycle between validation and save.
        Map<Long, Document> documents = documentRepository.findAllWithWriteLockByWorkSpaceId(workSpaceId)
                .stream().collect(Collectors.toMap(Document::getId, Function.identity()));
        Set<Long> selected = new HashSet<>(documentIds);
        for (Long id : selected) {
            Document document = documents.get(id);
            if (document == null || document.getType() == DocumentType.WORKSPACE_HOME) {
                throw new IllegalArgumentException("이 Workspace 의 Note 와 Task 만 이동할 수 있습니다");
            }
        }
        if (parentId != null) {
            Document parent = documents.get(parentId);
            if (parent == null || parent.getType() == DocumentType.WORKSPACE_HOME) {
                throw new IllegalArgumentException("이 Workspace 안의 이동 위치를 선택해야 합니다");
            }
            Set<Long> visited = new HashSet<>();
            while (parent != null) {
                if (selected.contains(parent.getId()) || !visited.add(parent.getId())) {
                    throw new IllegalArgumentException("문서를 자기 자신이나 하위 문서 아래로 이동할 수 없습니다");
                }
                parent = documents.get(parent.getParentId());
            }
        }
        // Resolve roots before changing parents; selected descendants stay with their selected ancestor.
        List<Document> roots = selected.stream().map(documents::get).filter(document -> {
            Long ancestorId = document.getParentId();
            Set<Long> visited = new HashSet<>();
            while (ancestorId != null && visited.add(ancestorId)) {
                if (selected.contains(ancestorId)) return false;
                Document ancestor = documents.get(ancestorId);
                ancestorId = ancestor == null ? null : ancestor.getParentId();
            }
            return true;
        }).toList();
        roots.forEach(document -> document.reparentTo(parentId));
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
        Document document = findForUpdate(documentId, type);
        validateParticipant(document.getWorkSpaceId(), memberId, type);
        String oldTitle = document.getTitle();
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
        if (!Objects.equals(oldTitle, document.getTitle())) {
            recordRefresh(document);
        }
    }

    @Transactional
    public void deleteDocument(final Long memberId, final Long documentId, final DocumentType type) {
        Document document = findForUpdate(documentId, type);
        validateParticipant(document.getWorkSpaceId(), memberId, type);

        if (!document.getAuthorId().equals(memberId)) {
            throw new ForbiddenException(type.deleteDeniedMessage());
        }

        imageUtils.deleteAllContentImages(document.getSearchContent());
        documentRepository.findAllByParentId(documentId).forEach(child -> {
            child.reparentTo(document.getParentId());
            documentRepository.save(child);
        });
        searchSyncRecorder.deleteDocument(documentId);
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
                    refreshRequests.recordDelta(document);
                    return new CommittedCrdtUpdate(
                            delta.getRevision(),
                            delta.getClientUpdateId(),
                            delta.getCrdtUpdate()
                    );
                });
    }

    private void recordRefresh(Document document) {
        searchSyncRecorder.refreshDocument(document.getId(),
                Objects.requireNonNullElse(document.getLatestRevision(), 0L));
    }

    private Document findForUpdate(final Long id, final DocumentType type) {
        return documentRepository.findWithLockByIdAndType(id, type)
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
