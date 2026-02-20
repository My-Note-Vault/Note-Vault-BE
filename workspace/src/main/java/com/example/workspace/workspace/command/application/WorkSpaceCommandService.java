package com.example.workspace.workspace.command.application;

import com.example.workspace.workspace.command.domain.Participant;
import com.example.workspace.workspace.command.domain.ParticipantRepository;
import com.example.workspace.workspace.command.domain.WorkSpace;
import com.example.workspace.workspace.command.domain.WorkSpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class WorkSpaceCommandService {

    private final WorkSpaceRepository workSpaceRepository;
    private final ParticipantRepository participantRepository;

    @Transactional
    public Long createWorkSpace(
            final Long memberId,
            final Long parentId,
            final String name,
            final String content,
            final Boolean isPublic
    ) {
        WorkSpace workSpace = new WorkSpace(memberId, parentId, name, content, isPublic);
        workSpaceRepository.save(workSpace);

        Participant participant = new Participant(workSpace.getId(), memberId);
        participantRepository.save(participant);

        return workSpace.getId();
    }


    @Transactional
    public void editWorkSpace(
            final Long memberId,
            final Long workSpaceId,
            final Long parentId,
            final String name,
            final String content,
            final Boolean isPublic
    ) {
        WorkSpace workSpace = workSpaceRepository.findById(workSpaceId)
                .orElseThrow(() -> new NoSuchElementException("WorkSpace 를 찾을 수 없습니다"));

        workSpace.edit(memberId, parentId, name, content, isPublic);
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
    public void leaveWorkSpace(
            final Long memberId,
            final Long workSpaceId
    ) {
        List<Participant> targetWorkSpaces = participantRepository.findByWorkSpaceIdAndMemberId(workSpaceId, memberId);
        participantRepository.delete(targetWorkSpaces.getFirst());
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
}
