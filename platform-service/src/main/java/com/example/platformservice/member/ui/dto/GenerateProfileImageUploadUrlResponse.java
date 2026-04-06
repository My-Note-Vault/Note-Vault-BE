package com.example.platformservice.member.ui.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class GenerateProfileImageUploadUrlResponse {

    private final String presignedUrl;
    private final String key;
}
