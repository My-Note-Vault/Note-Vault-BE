package com.example.platformservice.dailynote.domain;

import com.example.common.Auditable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "daily_note_folder",
        uniqueConstraints = @UniqueConstraint(columnNames = {"author_id", "name"}),
        indexes = @Index(columnList = "author_id")
)
@Entity
public class DailyNoteFolder extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 100)
    private String name;

    public DailyNoteFolder(final Long authorId, final String name) {
        this.authorId = authorId;
        rename(name);
    }

    public void rename(final String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("폴더 이름을 입력해야 합니다");
        }
        String trimmedName = name.trim();
        if (trimmedName.length() > 100) {
            throw new IllegalArgumentException("폴더 이름은 100자 이하여야 합니다");
        }
        this.name = trimmedName;
    }
}
