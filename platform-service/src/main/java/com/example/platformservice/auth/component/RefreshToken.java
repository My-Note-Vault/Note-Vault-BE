package com.example.platformservice.auth.component;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "refresh_token",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_refresh_token_member_id", columnNames = "member_id")
        }
)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 1000)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    private RefreshToken(
            final Long memberId,
            final String token,
            final LocalDateTime expiresAt
    ) {
        this.memberId = memberId;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public static RefreshToken create(
            final Long memberId,
            final String token,
            final LocalDateTime expiresAt
    ) {
        return new RefreshToken(memberId, token, expiresAt);
    }

    public void update(
            final String token,
            final LocalDateTime expiresAt
    ) {
        this.token = token;
        this.expiresAt = expiresAt;
    }
}
