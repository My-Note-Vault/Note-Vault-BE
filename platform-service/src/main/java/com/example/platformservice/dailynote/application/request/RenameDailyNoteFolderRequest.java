package com.example.platformservice.dailynote.application.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RenameDailyNoteFolderRequest {
    private final String name;
}
