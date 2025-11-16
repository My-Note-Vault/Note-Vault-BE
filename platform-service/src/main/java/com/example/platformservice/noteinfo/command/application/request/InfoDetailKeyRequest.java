package com.example.platformservice.noteinfo.command.application.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class InfoDetailKeyRequest {

    private final String snapshotImageKey;
    private final List<String> infoImageKeys;
}
