package com.example.platformservice.dailynote.application.response;

import java.util.List;

public record DailyNoteListResponse(
        List<DailyNoteFolderResponse> folders,
        List<DailyNoteSimpleResponse> notes
) {
}
