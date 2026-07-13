package com.example.workspace.document.command.application;

import com.example.common.exception.ForbiddenException;
import com.example.common.file.image.ImageUtils;
import com.example.workspace.document.command.domain.Document;
import com.example.workspace.document.command.domain.DocumentRepository;
import com.example.workspace.document.command.domain.DocumentType;
import com.example.workspace.task.command.domain.value.Status;
import com.example.workspace.workspace.command.domain.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class DocumentCommandService {

    private final DocumentRepository documentRepository;
    private final ParticipantRepository participantRepository;
    private final ImageUtils imageUtils;

    @Transactional
    public Long createDocument(
            final Long memberId,
            final DocumentType type,
            final Long workSpaceId,
            final Long parentId
    ) {
        Document document = switch (type) {
            case TASK -> {
                validateParticipant(workSpaceId, memberId, type);
                yield Document.task(workSpaceId, memberId);
            }
            case SUBTASK, NOTE -> {
                Document parent = findParent(parentId, type);
                validateParticipant(parent.getWorkSpaceId(), memberId, type);
                yield type == DocumentType.SUBTASK
                        ? Document.subTask(parent, memberId)
                        : Document.note(parent, memberId);
            }
        };
        documentRepository.save(document);
        return document.getId();
    }

    @Transactional
    public void editDocument(
            final Long memberId,
            final Long documentId,
            final DocumentType type,
            final Long parentId,
            final String title,
            final String content,
            final Status status,
            final LocalDateTime startDateTime,
            final LocalDateTime endDateTime,
            final Boolean isPublic
    ) {
        Document document = findByIdAndType(documentId, type);
        validateParticipant(document.getWorkSpaceId(), memberId, type);
        String oldContent = document.getContent();

        if (parentId != null) {
            Document parent = findParent(parentId, type);
            validateParticipant(parent.getWorkSpaceId(), memberId, type);
            document.moveTo(parent);
        }

        document.edit(title, content, status, startDateTime, endDateTime, isPublic);
        imageUtils.deleteRemovedContentImages(oldContent, content);
        documentRepository.save(document);
    }

    @Transactional
    public void deleteDocument(final Long memberId, final Long documentId, final DocumentType type) {
        Document document = findByIdAndType(documentId, type);
        validateParticipant(document.getWorkSpaceId(), memberId, type);

        if (!document.getAuthorId().equals(memberId)) {
            throw new ForbiddenException(type.deleteDeniedMessage());
        }

        imageUtils.deleteAllContentImages(document.getContent());
        documentRepository.delete(document);
    }

    private Document findByIdAndType(final Long id, final DocumentType type) {
        return documentRepository.findByIdAndType(id, type)
                .orElseThrow(() -> new NoSuchElementException(type.notFoundMessage()));
    }

    private Document findParent(final Long parentId, final DocumentType type) {
        if (!type.requiresParent() || parentId == null) {
            throw new NoSuchElementException(type.parentNotFoundMessage());
        }
        return documentRepository.findByIdAndType(parentId, type.parentType())
                .orElseThrow(() -> new NoSuchElementException(type.parentNotFoundMessage()));
    }

    private void validateParticipant(final Long workSpaceId, final Long memberId, final DocumentType type) {
        participantRepository.findByWorkSpaceIdAndMemberId(workSpaceId, memberId)
                .orElseThrow(() -> new ForbiddenException(type.accessDeniedMessage()));
    }
}
