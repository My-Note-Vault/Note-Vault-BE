package com.example.platformservice.member.ui.dto;

import java.time.LocalDateTime;

public record PayoutAccountVerificationResponse(
        String verificationToken,
        String bankName,
        String maskedAccountNumber,
        String maskedHolderName,
        LocalDateTime expiresAt
) {}
