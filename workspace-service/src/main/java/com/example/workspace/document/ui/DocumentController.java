package com.example.workspace.document.ui;

import com.example.common.AuthMemberId;
import com.example.workspace.document.command.application.DocumentCommandService;
import com.example.workspace.document.command.domain.DocumentType;
import com.example.workspace.document.command.domain.Document;
import com.example.workspace.document.query.DocumentQueryService;
import com.example.workspace.document.ui.request.CreateDocumentRequest;
import com.example.workspace.document.ui.request.EditDocumentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/v1/documents")
@RestController
public class DocumentController {

    private final DocumentCommandService documentCommandService;
    private final DocumentQueryService documentQueryService;

    @GetMapping("/{type}")
    public ResponseEntity<List<Document>> findDocuments(
            @PathVariable final DocumentType type,
            @AuthMemberId final Long memberId
    ) {
        return ResponseEntity.ok(documentQueryService.findAllByAuthorIdAndType(memberId, type));
    }

    @GetMapping("/{type}/{id}")
    public ResponseEntity<Document> findDocument(
            @PathVariable final DocumentType type,
            @PathVariable final Long id,
            @AuthMemberId final Long memberId
    ) {
        return ResponseEntity.ok(documentQueryService.findDocumentById(memberId, id, type));
    }

    @PostMapping
    public ResponseEntity<Long> createDocument(
            @RequestBody final CreateDocumentRequest request,
            @AuthMemberId final Long memberId
    ) {
        Long id = documentCommandService.createDocument(
                memberId,
                request.getType(),
                request.getWorkSpaceId(),
                request.getParentId()
        );
        applyInitialFields(request, memberId, id);
        return ResponseEntity.ok(id);
    }

    @PatchMapping("/{type}/{id}")
    public ResponseEntity<Void> editDocument(
            @PathVariable final DocumentType type,
            @PathVariable final Long id,
            @RequestBody final EditDocumentRequest request,
            @AuthMemberId final Long memberId
    ) {
        documentCommandService.editDocument(
                memberId,
                id,
                type,
                request.getParentId(),
                request.getTitle(),
                request.getContent(),
                request.getStatus(),
                request.getStartDateTime(),
                request.getEndDateTime(),
                request.getIsPublic()
        );
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{type}/{id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable final DocumentType type,
            @PathVariable final Long id,
            @AuthMemberId final Long memberId
    ) {
        documentCommandService.deleteDocument(memberId, id, type);
        return ResponseEntity.noContent().build();
    }

    private void applyInitialFields(
            final CreateDocumentRequest request,
            final Long memberId,
            final Long id
    ) {
        if (!hasInitialFields(request)) {
            return;
        }

        documentCommandService.editDocument(
                memberId,
                id,
                request.getType(),
                null,
                request.getTitle(),
                request.getContent(),
                request.getStatus(),
                request.getStartDateTime(),
                request.getEndDateTime(),
                request.getIsPublic()
        );
    }

    private boolean hasInitialFields(final CreateDocumentRequest request) {
        return request.getTitle() != null
                || request.getContent() != null
                || request.getStatus() != null
                || request.getStartDateTime() != null
                || request.getEndDateTime() != null
                || request.getIsPublic() != null;
    }
}
