package com.example.platformservice.dailynote.application.response;

import com.example.platformservice.dailynote.domain.Plan;
import com.example.platformservice.dailynote.domain.Type;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PlanResponse {

    private final Long planId;
    private final Type type;
    private final String content;
    private final Boolean isDone;

    public static PlanResponse from(Plan plan) {
        return new PlanResponse(plan.getId(), plan.getType(), plan.getContent(), plan.getIsDone());
    }
}
