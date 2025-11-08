package com.example.platformservice.noteinfo.command.domain;

import com.example.common.Auditable;
import com.example.platformservice.noteinfo.command.domain.value.Status;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "noteinfo",
        indexes = @Index(columnList = "snapshotId")
)
@Entity
public class NoteInfo extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long snapshotId;
    @Column(nullable = false)
    private Long authorId; // info 를 작성한 사람..(화면 표시 전용).. note 의 author 과는 다를 수 있음

    @Column(nullable = false)
    private String title;
    private String description;

    private BigDecimal price;

    private Long viewCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @OneToMany(mappedBy = "noteinfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NoteInfoCategory> categories = new ArrayList<>();

    @OneToMany(mappedBy = "noteinfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NoteInfoImage> images = new ArrayList<>();


    @Builder
    public NoteInfo(
            final Long snapshotId,
            final Long authorId,
            final String title,
            final String description,
            final BigDecimal price,
            final Status status,
            final List<NoteInfoCategory> categories
    ) {
        this.snapshotId = snapshotId;
        this.authorId = authorId;
        this.title = title;
        this.description = description;
        this.viewCount = 0L;
        this.status = status;
        this.price = new BigDecimal("0");

        if (status.equals(Status.FOR_SALE)) {
            this.price = price;
        }

        this.categories.addAll(categories);
    }

    public void updateWithoutImage(
            final String title,
            final String description,
            final BigDecimal price,
            final Status status,
            final List<NoteInfoCategory> categories
    ) {
        if (!this.title.equals(title)) {
            this.title = title;
        }
        if (!this.description.equals(description)) {
            this.description = description;
        }
        if (!this.price.equals(price)) {
            this.price = price;
        }
        if (!this.status.equals(status)) {
            this.status = status;
        }
        if (!this.categories.equals(categories)) {
            this.categories.clear();
            this.categories.addAll(categories);
        }
    }

    public void updateImages(final List<NoteInfoImage> images) {
        if (!this.images.equals(images)) {
            this.images.clear();
            this.images.addAll(images);
        }
    }

    public void uploadImages(final NoteInfoImage image) {
        this.images.add(image);
    }
}
