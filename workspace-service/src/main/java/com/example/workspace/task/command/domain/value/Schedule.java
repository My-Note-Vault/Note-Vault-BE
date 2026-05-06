package com.example.workspace.task.command.domain.value;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Schedule {

    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    public Schedule(Status status) {
        this(status, null, null);
    }

    public Schedule(LocalDateTime startDateTime) {
        this(Status.NOT_STARTED, startDateTime, null);
    }

    public Schedule(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        this(Status.NOT_STARTED, startDateTime, endDateTime);
    }

    public void edit(Status status, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        this.status = status == null ? this.status : status;
        this.startDateTime = startDateTime == null ? this.startDateTime : startDateTime;
        this.endDateTime = endDateTime == null ? this.endDateTime : endDateTime;
    }
}
