package com.example.common.file;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
public abstract class Image {


    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UploadStatus uploadStatus;

    @Column(nullable = false)
    private String key;

    protected Image(String key) {
        this.key = key;
        this.uploadStatus = UploadStatus.PENDING;
    }
}


