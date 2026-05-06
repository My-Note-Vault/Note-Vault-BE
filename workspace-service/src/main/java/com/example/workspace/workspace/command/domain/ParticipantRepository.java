package com.example.workspace.workspace.command.domain;

import com.example.workspace.workspace.query.response.InvitedWorkSpaceSummaryResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    Optional<Participant> findByWorkSpaceIdAndMemberId(Long workSpaceId, Long memberId);

    @Query("""
SELECT w.name, i.expiresAt
FROM Invitation i
JOIN WorkSpace w ON w.id = i.workSpaceId
WHERE i.code = :code
""")
    InvitedWorkSpaceSummaryResponse findWorkspaceSummaryByCode(String code);

    List<Participant> findByMemberId(Long memberId);
}
