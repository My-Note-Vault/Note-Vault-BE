package com.example.platformservice.member.infra;

import com.example.platformservice.member.domain.PayoutAccountVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface PayoutAccountVerificationRepository
        extends JpaRepository<PayoutAccountVerification, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PayoutAccountVerification> findByTokenAndMemberId(String token, Long memberId);
}
