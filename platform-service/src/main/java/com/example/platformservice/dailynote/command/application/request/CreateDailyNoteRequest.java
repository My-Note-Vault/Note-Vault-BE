package com.example.platformservice.dailynote.command.application.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateDailyNoteRequest {

    private final String todayTodoList;
    private final String tomorrowTodoList;
    private final String memo;

    @NotBlank
    private final Boolean isCollapsed;
}
