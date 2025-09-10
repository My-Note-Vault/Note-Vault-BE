package com.example.noteservice.noteinfo.domain;

import com.example.common.Auditable;
import com.example.noteservice.noteinfo.domain.value.Category;
import com.example.noteservice.noteinfo.domain.value.Status;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "note_info",
        indexes = {
                @Index(columnList = "noteId"),
                @Index(columnList = "authorId")
        }
)
@Entity
public class NoteInfo extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long noteId;
    private Long authorId;

    @Column(nullable = false)
    private String title;

    private String description;
    private BigDecimal price;

    private Long viewCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;


    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "note_category", joinColumns = @JoinColumn(name = "note_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private Set<Category> categories = new HashSet<>();


    @Builder
    public NoteInfo(
            final Long noteId,
            final Long authorId,
            final String title,
            final String description,
            final BigDecimal price,
            final Status status,
            final Set<Category> categories
    ) {
        this.noteId = noteId;
        this.authorId = authorId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.viewCount = 0L;
        this.status = status;
        this.categories = categories;
    }
}
