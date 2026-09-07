package com.example.platformservice.member.ui.dto;

import com.example.platformservice.member.domain.value.BankCode;

public record PayoutAccountResponse(
        boolean configured,
        BankCode bankCode,
        String bankName,
        String maskedAccountNumber
) {
    public static PayoutAccountResponse empty() {
        return new PayoutAccountResponse(false, null, null, null);
    }
}
