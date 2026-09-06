package com.example.platformservice.member.domain;

import com.example.platformservice.member.domain.value.BankCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PayoutAccountVerification {

    private static final int VALID_MINUTES = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String token;

    @Column(nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BankCode bankCode;

    @Column(nullable = false, length = 14)
    private String accountNumber;

    @Column(nullable = false, length = 50)
    private String holderName;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean consumed;

    public static PayoutAccountVerification create(
            final Long memberId,
            final BankCode bankCode,
            final String accountNumber,
            final String holderName
    ) {
        PayoutAccountVerification verification = new PayoutAccountVerification();
        verification.token = UUID.randomUUID().toString();
        verification.memberId = memberId;
        verification.bankCode = bankCode;
        verification.accountNumber = accountNumber;
        verification.holderName = holderName;
        verification.expiresAt = LocalDateTime.now().plusMinutes(VALID_MINUTES);
        verification.consumed = false;
        return verification;
    }

    public void validateUsable() {
        if (consumed) {
            throw new IllegalArgumentException("이미 사용된 계좌 인증입니다");
        }
        if (expiresAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("계좌 인증이 만료되었습니다. 다시 인증해 주세요");
        }
    }

    public void consume() {
        validateUsable();
        consumed = true;
    }
}
