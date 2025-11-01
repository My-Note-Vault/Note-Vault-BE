package com.example.common.file;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UploadImageResponse {

    private final String presignedUrl;
    private final String key;
}
