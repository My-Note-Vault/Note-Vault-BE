package com.example.platformservice.dailynote.command.domain;

import com.example.common.Auditable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.NoSuchElementException;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "daily_note",
        indexes = @Index(columnList = "authorId")
)
@Entity
public class DailyNote extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long authorId;

    private String todayTodoList;
    private String tomorrowTodoList;

    private String memo;

    private Boolean isCollapsed;


    public DailyNote(
            final Long authorId,
            final String todayTodoList,
            final String tomorrowTodoList,
            final String memo,
            final Boolean isCollapsed
    ) {
        this.authorId = authorId;
        this.todayTodoList = todayTodoList;
        this.tomorrowTodoList = tomorrowTodoList;
        this.memo = memo;

        this.isCollapsed = Objects.requireNonNullElse(isCollapsed, false);
    }

    public void edit(
            final Long authorId,
            final String todayTodoList,
            final String tomorrowTodoList,
            final String memo,
            final Boolean isCollapsed
    ) {
        if (!this.authorId.equals(authorId)) {
            throw new NoSuchElementException("자신의 노트가 아닙니다!");
        }
        this.todayTodoList = todayTodoList == null ? this.todayTodoList : todayTodoList;
        this.tomorrowTodoList = tomorrowTodoList == null ? this.tomorrowTodoList : tomorrowTodoList;
        this.memo = memo == null ? this.memo : memo;
        this.isCollapsed = isCollapsed == null ? this.isCollapsed : isCollapsed;
    }


}
