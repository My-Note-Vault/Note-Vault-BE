package com.example.platformservice.noteinfo.query.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateNoteInfoResponse {

    private final Long noteInfoId;

    private final String previewImageUrl;
    private final String blurredImageUrl;
}
