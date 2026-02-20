package com.example.workspace.workspace.command.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    List<Participant> findByWorkSpaceIdAndMemberId(Long workSpaceId, Long memberId);
}
