package com.example.workspace.workspace.query;

import com.example.workspace.document.command.domain.Document;
import com.example.workspace.document.command.domain.DocumentRepository;
import com.example.workspace.document.command.domain.DocumentType;
import com.example.workspace.workspace.command.domain.Participant;
import com.example.workspace.workspace.command.domain.ParticipantRepository;
import com.example.workspace.workspace.command.domain.WorkSpace;
import com.example.workspace.workspace.command.domain.WorkSpaceRepository;
import com.example.workspace.workspace.query.response.InvitedWorkSpaceSummaryResponse;
import com.example.workspace.workspace.query.response.WorkspaceDocumentTreeNodeResponse;
import com.example.workspace.workspace.query.response.WorkSpaceSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static com.example.workspace.common.WorkspaceConst.NO_PARTICIPANT_MESSAGE;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class WorkSpaceQueryService {

    private final WorkSpaceRepository workSpaceRepository;
    private final ParticipantRepository participantRepository;
    private final DocumentRepository documentRepository;

    public List<WorkspaceDocumentTreeNodeResponse> findDocumentTree(
            final Long memberId,
            final Long workSpaceId
    ) {
        participantRepository.findByWorkSpaceIdAndMemberId(workSpaceId, memberId)
                .orElseThrow(() -> new NoSuchElementException(NO_PARTICIPANT_MESSAGE));

        List<Document> documents = documentRepository.findAllByWorkSpaceIdAndTypeIn(
                workSpaceId,
                List.of(DocumentType.TASK, DocumentType.NOTE)
        );
        Map<Long, WorkspaceDocumentTreeNodeResponse> nodes = new LinkedHashMap<>();
        for (Document document : documents) {
            nodes.put(document.getId(), new WorkspaceDocumentTreeNodeResponse(
                    document.getId(),
                    document.getType(),
                    document.getTitle(),
                    document.getParentId(),
                    new ArrayList<>()
            ));
        }

        List<WorkspaceDocumentTreeNodeResponse> roots = new ArrayList<>();
        for (WorkspaceDocumentTreeNodeResponse node : nodes.values()) {
            WorkspaceDocumentTreeNodeResponse parent = node.parentId() == null
                    ? null
                    : nodes.get(node.parentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.children().add(node);
            }
        }
        return roots;
    }

    public WorkSpace findWorkSpaceById(final Long authorId, final Long workSpaceId) {
        participantRepository.findByWorkSpaceIdAndMemberId(workSpaceId, authorId)
                .orElseThrow(() -> new NoSuchElementException(NO_PARTICIPANT_MESSAGE));

        return workSpaceRepository.findById(workSpaceId)
                .orElseThrow(() -> new NoSuchElementException("WorkSpace 를 찾을 수 없습니다"));

    }

    public String findWorkSpaceLastPath(final Long workSpaceId, final Long memberId) {
        Participant participant = participantRepository.findByWorkSpaceIdAndMemberId(workSpaceId, memberId)
                .orElseThrow(() -> new NoSuchElementException(NO_PARTICIPANT_MESSAGE));

        return participant.getLastVisitedPath();
    }

    public List<WorkSpaceSummaryResponse> findAllWorkSpacesByCreatorId(final Long memberId) {
        List<Long> participatingWorkSpaceIds = participantRepository.findByMemberId(memberId).stream()
                .map(Participant::getWorkSpaceId)
                .toList();

        return workSpaceRepository.findByIdIn(participatingWorkSpaceIds).stream()
                .map(w -> new WorkSpaceSummaryResponse(w.getId(), w.getName(), w.getIsPublic()))
                .toList();
    }

    public InvitedWorkSpaceSummaryResponse findInvitedWorkSpaceSummary(final String code) {
        return participantRepository.findWorkspaceSummaryByCode(code);
    }


}
