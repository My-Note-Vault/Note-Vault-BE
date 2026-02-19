package com.example.workspace.task.command.domain;

import lombok.Getter;

@Getter
public enum Status {
    NOT_STARTED("대기"),
    IN_PROGRESS("진행 중"),
    COMPLETED("완료");

    private final String description;

    Status(String description) {
        this.description = description;
    }

}
