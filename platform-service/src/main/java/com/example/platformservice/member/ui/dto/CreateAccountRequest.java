package com.example.platformservice.member.ui.dto;

import com.example.platformservice.member.domain.value.BankCode;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateAccountRequest {

    @NotBlank
    private final BankCode bankCode;
    @NotBlank
    private final String accountNumber;
    @NotBlank
    private final String accountHolderName;

}
