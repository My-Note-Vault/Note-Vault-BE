package com.example.platformservice.dailynote.application.request;

import com.example.platformservice.dailynote.domain.Type;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EditPlanRequest {

    private final Long planId;
    private final Type type;
    private final String content;
    private final Boolean isDone;
}
