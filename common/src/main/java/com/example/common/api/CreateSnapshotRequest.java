package com.example.common.api;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor
public class CreateSnapshotRequest {

    private final Long noteId;
    private final String previewImageKey;
    private final String blurredImageKey;
    private final String title;
    private final String content;

}
