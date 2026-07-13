package com.example.workspace.document.query;

import com.example.common.exception.ForbiddenException;
import com.example.workspace.document.command.domain.DocumentType;
import com.example.workspace.document.command.domain.Document;
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

    private void validateParticipant(final Long workSpaceId, final Long memberId, final DocumentType type) {
        participantRepository.findByWorkSpaceIdAndMemberId(workSpaceId, memberId)
                .orElseThrow(() -> new ForbiddenException(type.accessDeniedMessage()));
    }
}
