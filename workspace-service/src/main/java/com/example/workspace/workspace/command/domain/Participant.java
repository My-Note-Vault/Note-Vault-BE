package com.example.workspace.workspace.command.domain;

import com.example.workspace.workspace.command.domain.value.ParticipantRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "workspace_member",
        indexes = @Index(columnList = "workspace_id")
)
@Entity
public class Participant {

    private static final String DEFAULT_LAST_VISITED_PATH_PREFIX = "/api/v1/workspaces";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id")
    private Long workSpaceId;
    private Long memberId;

    @Enumerated(EnumType.STRING)
    private ParticipantRole role;

    @Setter
    private String lastVisitedPath;

    public Participant(final Long workSpaceId, final Long memberId) {
        this.workSpaceId = workSpaceId;
        this.memberId = memberId;
        this.role = ParticipantRole.MEMBER;
        this.lastVisitedPath = DEFAULT_LAST_VISITED_PATH_PREFIX + "/" + workSpaceId;
    }
}
