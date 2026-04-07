package com.example.platformservice.member.ui.dto;

import com.example.platformservice.member.domain.value.DayStartTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MemberProfileResponse {

    private final String nickname;
    private final String profileImageKey;
    private final DayStartTime dayStartTime;
}
