package com.example.platformservice.dailynote.application.response;

import com.example.platformservice.dailynote.domain.DailyNoteFolder;

public record DailyNoteFolderResponse(Long folderId, String name) {
    public static DailyNoteFolderResponse from(final DailyNoteFolder folder) {
        return new DailyNoteFolderResponse(folder.getId(), folder.getName());
    }
}
