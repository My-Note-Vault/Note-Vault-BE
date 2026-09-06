package com.example.platformservice.member.ui.dto;

import com.example.platformservice.member.domain.value.BankCode;

public record PayoutAccountResponse(
        boolean configured,
        boolean verified,
        BankCode bankCode,
        String bankName,
        String maskedAccountNumber,
        String maskedHolderName
) {
    public static PayoutAccountResponse empty() {
        return new PayoutAccountResponse(false, false, null, null, null, null);
    }
}
