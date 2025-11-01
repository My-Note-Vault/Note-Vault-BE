package com.example.platformservice.member.domain;

import com.example.common.Auditable;
import com.example.platformservice.member.domain.value.Provider;
import com.example.platformservice.member.domain.value.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Member extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private String name;

    private String nickname;
    private String profileImageKey;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;
    @Column(nullable = false)
    private String providerUserId;


    private Member(
            final Role role,
            final String name,
            final String nickname,
            final Provider provider,
            final String providerUserId,
            final String email,
            final String profileImageKey
    ) {
        this.role = role;
        this.name = name;
        this.nickname = nickname;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.profileImageKey = profileImageKey;
    }

    public static Member googleSignUp(
            final String email,
            final String name,
            final String providerUserId
    ) {
        return new Member(
                Role.USER,
                name,
                "",
                Provider.GOOGLE,
                providerUserId,
                email,
                ""
        );
    }

    public void completeProfile(final String nickname, final String profileImageUrl) {
        this.nickname = nickname;
        this.profileImageKey = profileImageUrl;
    }
}
