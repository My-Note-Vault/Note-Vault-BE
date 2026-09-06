package com.example.platformservice.member.domain;

import com.example.common.Auditable;
import com.example.platformservice.member.domain.value.DayStartTime;
import com.example.platformservice.member.domain.value.BankCode;
import com.example.platformservice.member.domain.value.PayoutAccountStatus;
import com.example.platformservice.member.domain.value.Provider;
import com.example.platformservice.member.domain.value.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static com.example.platformservice.PlatformConst.DAILY_NOTES_BASIC_PATH;

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

    @Column(nullable = false, unique = true, length = 6)
    private String memberTag;

    private String profileImageKey;

    @Enumerated(EnumType.STRING)
    private BankCode payoutBankCode;

    private String payoutAccountNumber;

    @Enumerated(EnumType.STRING)
    private PayoutAccountStatus payoutAccountStatus;

    private String payoutAccountHolderName;
    private java.time.LocalDateTime payoutAccountVerifiedAt;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;
    @Column(nullable = false)
    private String providerUserId;

    @Embedded
    private DayStartTime dayStartTime;

    @Setter
    private String lastVisitedPath;


    private Member(
            final Role role,
            final String name,
            final String nickname,
            final String memberTag,
            final Provider provider,
            final String providerUserId,
            final String email,
            final String profileImageKey
    ) {
        this.role = role;
        this.name = name;
        this.nickname = nickname;
        this.memberTag = memberTag;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.profileImageKey = profileImageKey;
        this.dayStartTime = DayStartTime.MIDNIGHT;
        this.lastVisitedPath = DAILY_NOTES_BASIC_PATH;
    }

    public static Member googleSignUp(
            final String email,
            final String name,
            final String providerUserId,
            final String nickname,
            final String memberTag
    ) {
        return new Member(
                Role.USER,
                name,
                nickname,
                memberTag,
                Provider.GOOGLE,
                providerUserId,
                email,
                ""
        );
    }

    /**
     * 개발 도구와 기존 호출부 호환용 팩토리다. 실제 OAuth 가입에서는 생성기를 통해
     * 닉네임과 태그를 전달하는 오버로드를 사용한다.
     */
    public static Member googleSignUp(
            final String email,
            final String name,
            final String providerUserId
    ) {
        String legacyTag = String.format("%06X", providerUserId.hashCode() & 0xFFFFFF);
        return googleSignUp(email, name, providerUserId, "", legacyTag);
    }

    public void completeProfile(final String nickname, final int dayStartHour, final int datStartMinute) {
        this.nickname = nickname.trim();
        this.dayStartTime = new DayStartTime(dayStartHour, datStartMinute);
    }

    public void assignMemberTag(final String memberTag) {
        if (this.memberTag == null || this.memberTag.isBlank()) {
            this.memberTag = memberTag;
        }
    }

    public void updateProfileImageKey(final String profileImageKey) {
        this.profileImageKey = profileImageKey;
    }

    public void updateVerifiedPayoutAccount(
            final BankCode bankCode,
            final String accountNumber,
            final String holderName
    ) {
        this.payoutBankCode = bankCode;
        this.payoutAccountNumber = accountNumber;
        this.payoutAccountStatus = PayoutAccountStatus.VERIFIED;
        this.payoutAccountHolderName = holderName;
        this.payoutAccountVerifiedAt = java.time.LocalDateTime.now();
    }

    public boolean hasPayoutAccount() {
        return payoutBankCode != null
                && payoutAccountNumber != null
                && !payoutAccountNumber.isBlank();
    }

    public boolean hasVerifiedPayoutAccount() {
        return hasPayoutAccount() && payoutAccountStatus == PayoutAccountStatus.VERIFIED;
    }

    public void deletePayoutAccount() {
        this.payoutBankCode = null;
        this.payoutAccountNumber = null;
        this.payoutAccountStatus = null;
        this.payoutAccountHolderName = null;
        this.payoutAccountVerifiedAt = null;
    }

}
