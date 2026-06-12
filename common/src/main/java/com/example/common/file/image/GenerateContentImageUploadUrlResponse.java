package com.example.common.file.image;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class GenerateContentImageUploadUrlResponse {

    private final String presignedUrl;
    private final String key;
}
