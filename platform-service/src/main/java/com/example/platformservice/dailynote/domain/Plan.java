package com.example.platformservice.dailynote.domain;

import com.example.common.Auditable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "plan",
        indexes = @Index(columnList = "daily_note_id")
)
@Entity
public class Plan extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Type type;

    private String content;

    private boolean isDone;

    public Plan(final Type type, final String content) {
        this.type = type;
        this.content = content;
    }

    public void edit(final Type type, final String content, final Boolean isDone) {
        this.type = (type == null ? this.type : type);
        this.content = (content == null ? this.content : content);
        this.isDone = (isDone == null ? this.isDone : isDone);
    }
}
