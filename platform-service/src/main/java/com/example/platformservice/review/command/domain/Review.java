package com.example.platformservice.review.command.domain;

import com.example.common.Auditable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "review",
        indexes = {
                @Index(columnList = "reviewerId"),
                @Index(columnList = "noteInfoId")
        }
)
@Entity
public class Review extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long reviewerId;
    private Long noteInfoId;

    private Integer rating;
    private String content;

    public Review(
            final Long reviewerId,
            final Long noteInfoId,
            final Integer rating
    ) {
        this(reviewerId, noteInfoId, rating, "");
    }

    public Review(
            final Long reviewerId,
            final Long noteInfoId,
            final Integer rating,
            final String content
    ) {
        this.reviewerId = reviewerId;
        this.noteInfoId = noteInfoId;
        this.rating = rating;
        this.content = content;
    }
}
