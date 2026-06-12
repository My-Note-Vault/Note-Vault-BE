package com.example.common.file.image;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class GenerateContentImageUploadUrlRequest {

    private final String targetType;

    private final String contentType;
}
