package com.example.workspace.workspace.command.application;

import com.example.workspace.common.WorkspaceConst;
import com.example.workspace.workspace.command.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static com.example.workspace.common.WorkspaceConst.EXPIRED_INVITATION_MESSAGE;
import static com.example.workspace.common.WorkspaceConst.NO_INVITATION_MESSAGE;

@RequiredArgsConstructor
@Service
public class WorkSpaceCommandService {

    private final WorkSpaceRepository workSpaceRepository;
    private final ParticipantRepository participantRepository;
    private final InvitationRepository invitationRepository;

    @Transactional
    public Long createWorkSpace(
            final Long memberId,
            final String name,
            final String content,
            final Boolean isPublic
    ) {
        WorkSpace workSpace = new WorkSpace(memberId, name, content, isPublic);
        workSpaceRepository.save(workSpace);

        Participant participant = new Participant(workSpace.getId(), memberId);
        participantRepository.save(participant);

        return workSpace.getId();
    }


    @Transactional
    public void editWorkSpace(
            final Long memberId,
            final Long workSpaceId,
            final String name,
            final String content,
            final Boolean isPublic
    ) {
        WorkSpace workSpace = workSpaceRepository.findById(workSpaceId)
                .orElseThrow(() -> new NoSuchElementException("WorkSpace 를 찾을 수 없습니다"));

        workSpace.edit(memberId, name, content, isPublic);
    }

    // creator 만 사용 가능
    @Transactional
    public void updateParticipants(
            final Long creatorId,
            final Long workSpaceId,
            final List<Long> memberIdsToAdd,
            final List<Long> memberIdsToRemove
    ) {
        WorkSpace workSpace = workSpaceRepository.findById(workSpaceId)
                .orElseThrow(() -> new NoSuchElementException("WorkSpace를 찾을 수 없습니다"));
        if (!workSpace.getCreatorId().equals(creatorId)) {
            throw new IllegalArgumentException("권한이 부족합니다");
        }

        List<Participant> membersToAdd = memberIdsToAdd.stream()
                .map(memberId -> new Participant(workSpaceId, memberId))
                .toList();
        participantRepository.saveAll(membersToAdd);

        List<Participant> membersToRemove = memberIdsToRemove.stream()
                .map(memberId -> new Participant(workSpaceId, memberId))
                .toList();
        participantRepository.deleteAll(membersToRemove);
    }

    @Transactional
    public void updateLastVisitedPath(final Long workSpaceId, final Long memberId, final String lastVisitedPath) {
        Participant participant = participantRepository.findByWorkSpaceIdAndMemberId(workSpaceId, memberId)
                .orElseThrow(() -> new NoSuchElementException(WorkspaceConst.NO_PARTICIPANT_MESSAGE));

        participant.setLastVisitedPath(lastVisitedPath);
    }

    @Transactional
    public void leaveWorkSpace(
            final Long memberId,
            final Long workSpaceId
    ) {
        Participant participant = participantRepository.findByWorkSpaceIdAndMemberId(workSpaceId, memberId)
                .orElseThrow(() -> new NoSuchElementException(WorkspaceConst.NO_PARTICIPANT_MESSAGE));

        participantRepository.delete(participant);
    }

    @Transactional
    public void deleteWorkSpace(final Long memberId, final Long workSpaceId) {
        WorkSpace workSpace = workSpaceRepository.findById(workSpaceId)
                .orElseThrow(() -> new NoSuchElementException("WorkSpace 를 찾을 수 없습니다"));

        if (!workSpace.getCreatorId().equals(memberId)) {
            throw new IllegalArgumentException("삭제할 권한이 없습니다.");
        }
        workSpaceRepository.delete(workSpace);
    }

    @Transactional
    public String createInvitationLink(final Long workSpaceId) {
        Invitation invitation = new Invitation(workSpaceId);
        invitationRepository.save(invitation);

        return invitation.getCode();
    }

    @Transactional
    public void acceptInvitation(final Long memberId, final String code) {
        Invitation invitation = invitationRepository.findByCode(code)
                .orElseThrow(() -> new NoSuchElementException(NO_INVITATION_MESSAGE));

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new NoSuchElementException(EXPIRED_INVITATION_MESSAGE);
        }

        Participant participant = new Participant(invitation.getWorkSpaceId(), memberId);
        participantRepository.save(participant);
    }
}
