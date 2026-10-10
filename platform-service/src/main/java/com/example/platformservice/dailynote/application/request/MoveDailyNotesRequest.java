package com.example.platformservice.dailynote.application.request;

import java.util.List;

public record MoveDailyNotesRequest(List<Long> dailyNoteIds, Long folderId) {
}
