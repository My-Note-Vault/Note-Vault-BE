package com.example.platformservice.member.ui.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CompleteProfileRequest {

    @NotBlank
    private final String nickname;
}
