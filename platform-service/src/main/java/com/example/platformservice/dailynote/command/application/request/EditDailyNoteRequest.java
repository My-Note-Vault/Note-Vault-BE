package com.example.platformservice.dailynote.command.application.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EditDailyNoteRequest {

    @NotBlank
    private final Long dailyNoteId;
    @NotBlank
    private final String todayTodoList;
    @NotBlank
    private final String tomorrowTodoList;
    @NotBlank
    private final String memo;
    @NotBlank
    private final Boolean isCollapsed;

}
