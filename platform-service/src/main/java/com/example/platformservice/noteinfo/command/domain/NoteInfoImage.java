package com.example.platformservice.noteinfo.command.domain;

import com.example.common.file.Image;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "noteinfo_image",
        indexes = @Index(columnList = "noteId")
)
@SQLDelete(sql = "UPDATE noteinfo_image SET upload_status = DELETED WHERE id = ?")
@Entity
public class NoteInfoImage extends Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "noteinfo_id", nullable = false)
    private NoteInfo noteInfo;


    public NoteInfoImage(final String imageKey) {
        super(imageKey);
    }

    public NoteInfoImage(final String imageKey, final NoteInfo noteInfo) {
        super(imageKey);
        this.noteInfo = noteInfo;
    }
}
