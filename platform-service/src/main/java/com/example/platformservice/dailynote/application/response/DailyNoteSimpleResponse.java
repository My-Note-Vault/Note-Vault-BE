package com.example.platformservice.dailynote.application.response;

import com.example.platformservice.dailynote.domain.DailyNote;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Getter
@RequiredArgsConstructor
public class DailyNoteSimpleResponse {

    private final Long dailyNoteId;
    private final LocalDate logicalDate;

    public static DailyNoteSimpleResponse from(DailyNote dailyNote) {
        return new DailyNoteSimpleResponse(dailyNote.getId(), dailyNote.getLogicalDate());
    }
}
