package com.example.workspace.workspace.query;

import com.example.workspace.workspace.command.domain.Participant;
import com.example.workspace.workspace.command.domain.ParticipantRepository;
import com.example.workspace.workspace.command.domain.WorkSpace;
import com.example.workspace.workspace.command.domain.WorkSpaceRepository;
import com.example.workspace.workspace.query.response.InvitedWorkSpaceSummaryResponse;
import com.example.workspace.workspace.query.response.WorkSpaceSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

import static com.example.workspace.common.WorkspaceConst.NO_PARTICIPANT_MESSAGE;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class WorkSpaceQueryService {

    private final WorkSpaceRepository workSpaceRepository;
    private final ParticipantRepository participantRepository;

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
