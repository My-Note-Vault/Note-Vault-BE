package com.example.workspace.workspace.command.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        indexes = @Index(columnList = "workspace_id")
)
@Entity
public class Invitation {

    private static final int DEFAULT_EXPIRED_DAYS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id")
    private Long workSpaceId;

    @Column(unique = true)
    private String code;

    private LocalDateTime expiresAt;

    public Invitation(final Long workSpaceId) {
        this(workSpaceId, LocalDateTime.now().plusDays(DEFAULT_EXPIRED_DAYS));
    }

    public Invitation(final Long workSpaceId, final LocalDateTime expiredAt) {
        this.workSpaceId = workSpaceId;
        this.expiresAt = expiredAt;

        this.code = UUID.randomUUID().toString();
    }
}
