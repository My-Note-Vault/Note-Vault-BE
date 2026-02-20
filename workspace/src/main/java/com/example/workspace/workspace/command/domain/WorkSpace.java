package com.example.workspace.workspace.command.domain;

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
        name = "workspace",
        indexes = @Index(columnList = "authorId")
)
@Entity
public class WorkSpace extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long creatorId;

    private Long parentId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    private Boolean isPublic;


    public WorkSpace(
            final Long creatorId,
            final Long parentId,
            final String name,
            final String content,
            final Boolean isPublic
    ) {
        this.creatorId = creatorId;
        this.parentId = parentId;
        this.name = name;
        this.content = content;

        this.isPublic = Objects.requireNonNullElse(isPublic, false);

    }

    public void edit(
            final Long creatorId,
            final Long parentId,
            final String name,
            final String content,
            final Boolean isPublic
    ) {
        if (!this.creatorId.equals(creatorId)) {
            throw new NoSuchElementException("자신의 WorkSpace가 아닙니다!");
        }
        this.parentId = parentId == null ? this.parentId : parentId;
        this.name = name == null ? this.name : name;
        this.content = content == null ? this.content : content;
        this.isPublic = isPublic == null ? this.isPublic : isPublic;
    }


}
