package com.example.platformservice.member.ui.dto;

import com.example.platformservice.member.domain.value.DayStartTime;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CompleteProfileRequest {

    @NotBlank
    private final String nickname;
    @NotBlank
    private final String profileImageKey;

    private final DayStartTime dayStartTime;

}
