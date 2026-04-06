package com.example.platformservice.member.ui.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ProfileImageResponse {

    private final String profileImageUrl;
    private final String profileImageKey;
}
