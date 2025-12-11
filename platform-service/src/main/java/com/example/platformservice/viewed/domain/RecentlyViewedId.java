package com.example.platformservice.viewed.domain;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@EqualsAndHashCode(of = { "memberId", "noteInfoId" })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class RecentlyViewedId implements Serializable {

    private Long memberId;
    private Long noteInfoId;

    public RecentlyViewedId(final Long memberId, final Long noteInfoId) {
        this.memberId = memberId;
        this.noteInfoId = noteInfoId;
    }
}
