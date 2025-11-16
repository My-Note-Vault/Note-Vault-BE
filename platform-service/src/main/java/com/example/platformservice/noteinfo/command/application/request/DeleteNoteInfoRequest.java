package com.example.platformservice.noteinfo.command.application.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DeleteNoteInfoRequest {

    private final Long noteInfoId;
}
