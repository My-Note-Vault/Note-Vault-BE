package com.example.workspace.workspace.command.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "participant",
        indexes = @Index(columnList = "workspaceId")
)
@Entity
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long workSpaceId;
    private Long memberId;

    public Participant(final Long workSpaceId, final Long memberId) {
        this.workSpaceId = workSpaceId;
        this.memberId = memberId;
    }
}
