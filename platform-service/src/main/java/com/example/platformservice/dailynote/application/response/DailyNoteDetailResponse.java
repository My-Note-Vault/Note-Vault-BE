package com.example.platformservice.dailynote.application.response;

import com.example.platformservice.dailynote.domain.DailyNote;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class DailyNoteDetailResponse {

    private final Long dailyNoteId;
    private final String content;
    private final LocalDate logicalDate;
    private final List<PlanResponse> plans;

    public static DailyNoteDetailResponse from(DailyNote dailyNote, List<PlanResponse> incompletePlans) {
        return new DailyNoteDetailResponse(
                dailyNote.getId(),
                dailyNote.getContent(),
                dailyNote.getLogicalDate(),
                incompletePlans
        );
    }
}
