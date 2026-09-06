package com.example.platformservice.member.ui.dto;

import com.example.platformservice.member.domain.value.BankCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UpdatePayoutAccountRequest {

    @NotNull
    private final BankCode bankCode;

    @NotBlank
    @Pattern(regexp = "^[0-9-]{6,18}$", message = "계좌번호 형식을 확인해 주세요")
    private final String accountNumber;
}
