package com.example.common.file.image;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ContentImageAccessResponse {

    private final boolean cloudFrontEnabled;
    private final String cdnBaseUrl;
    private final long expiresInSeconds;
}
