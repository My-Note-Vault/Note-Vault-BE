package com.example.platformservice.member.ui.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CompleteProfileRequest {

    @NotBlank
    private final String nickname;
    @NotBlank
    private final String profileImageUrl;


    /**
     * 사용자가 선택을 원하지 않으면 기본 닉네임과 기본 이미지를 전송받는다..
     */
}
