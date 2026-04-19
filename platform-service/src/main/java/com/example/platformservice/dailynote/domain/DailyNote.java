package com.example.platformservice.dailynote.domain;

import com.example.common.Auditable;
import com.example.platformservice.member.domain.value.DayStartTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "daily_note",
        indexes = @Index(columnList = "author_id")
)
@Entity
public class DailyNote extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long authorId;

    private LocalDate logicalDate;

    private String content;

    public DailyNote(final Long authorId, final DayStartTime dayStartTime) {
        this(authorId, "", dayStartTime);
    }

    public DailyNote(final Long authorId, final String content, final DayStartTime dayStartTime) {
        this.authorId = authorId;
        this.content = content;

        LocalTime criteriaTime = LocalTime.of(dayStartTime.getHour(), dayStartTime.getMinute(), 0);
        this.logicalDate = convertToLogicalToday(criteriaTime);
    }

    public void edit(final String content) {
        this.content = (content == null ? this.content : content);
    }

    public static LocalDate convertToLogicalToday(final DayStartTime dayStartTime) {
        LocalTime dayStartLocalTime = LocalTime.of(dayStartTime.getHour(), dayStartTime.getMinute(), 0);
        return convertToLogicalToday(dayStartLocalTime);
    }

    public static LocalDate convertToLogicalToday(final LocalTime criteriaTime) {
        LocalDate nowDate = LocalDate.now();
        LocalTime nowTime = LocalTime.now();

        if (nowTime.isBefore(criteriaTime)) {
            return nowDate.minusDays(1);
        }
        return nowDate;
    }
}
