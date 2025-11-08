package com.example.platformservice.noteinfo.command.domain;

import com.example.platformservice.noteinfo.command.domain.value.Category;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "category")
@Table(
        name = "category",
        indexes = @Index(columnList = "noteId")
)
@Entity
public class NoteInfoCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "noteinfo_id", nullable = false)
    private NoteInfo noteInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    public NoteInfoCategory(final Category category) {
        this.category = category;
    }

    public NoteInfoCategory(final NoteInfo noteInfo, final Category category) {
        this.noteInfo = noteInfo;
        this.category = category;
    }
}
