package com.example.platformservice.noteinfo.command.application.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DeleteNoteInfoImageRequest {

    private final Long noteInfoId;
    private final Integer imageIndex;
}
