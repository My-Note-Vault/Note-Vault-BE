package com.example.platformservice.member.ui.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CompleteProfileRequest {

    @NotBlank
    @Size(max = 20)
    @Pattern(regexp = "^[^#]+$", message = "닉네임에는 #을 사용할 수 없습니다")
    private final String nickname;

    private final int dayStartHour;
    private final int dayStartMinute;

}
