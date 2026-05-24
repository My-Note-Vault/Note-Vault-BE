package com.example.workspace.calendar.ui.response;

import java.time.LocalDate;

public record DateEventResponse(
        String type,

        Long id,

        String title,
        String status,

        LocalDate startDate,
        LocalDate endDate,

        Parent parentTask
) {

    public record Parent(
            Long id,

            String title,
            String status,

            LocalDate startDate,
            LocalDate endDate
    ) {
    }
}