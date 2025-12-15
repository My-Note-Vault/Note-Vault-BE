package com.example.platformservice.viewed.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "recently_viewed_info"
)
@Entity
public class RecentlyViewedInfo {

    @EmbeddedId
    private RecentlyViewedId id;

    @Column(nullable = false)
    private LocalDateTime viewedAt;


    public RecentlyViewedInfo(final RecentlyViewedId id) {
        this.id = id;
        this.viewedAt = LocalDateTime.now();
    }

    public void update() {
        this.viewedAt = LocalDateTime.now();
    }
}
