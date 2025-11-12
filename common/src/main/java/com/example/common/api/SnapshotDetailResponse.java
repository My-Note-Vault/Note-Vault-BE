package com.example.common.api;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@RequiredArgsConstructor
public class SnapshotDetailResponse {

    private final Long snapshotId;

    private final Long authorId;

    private final String title;
    private final String content;

    private final String previewImageKey;
    private final String blurredImageKey;

    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

}
