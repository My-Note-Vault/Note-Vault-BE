package com.example.platformservice.like.domain;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "likes",
        indexes = {
                @Index(columnList = "memberId"),
                @Index(columnList = "noteInfoId, memberId")
        }
)
@Entity
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;
    private Long noteInfoId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;


    public Like(final Long memberId, final Long noteInfoId) {
        this.memberId = memberId;
        this.noteInfoId = noteInfoId;
    }
}
