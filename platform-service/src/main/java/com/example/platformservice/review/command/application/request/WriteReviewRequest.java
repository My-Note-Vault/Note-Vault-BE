package com.example.platformservice.review.command.application.request;

import lombok.Getter;
import org.hibernate.validator.constraints.Range;

@Getter
public class WriteReviewRequest {

    private final Long noteInfoId;

    @Range(min = 1, max = 5)
    private final Integer rating;
    private final String content;


    public WriteReviewRequest(
            final Long noteInfoId,
            final Integer rating
    ) {
        this(noteInfoId, rating, "");
    }

    public WriteReviewRequest(
            final Long noteInfoId,
            final Integer rating,
            final String content
    ) {
        this.noteInfoId = noteInfoId;
        this.rating = rating;
        this.content = content;
    }
}
