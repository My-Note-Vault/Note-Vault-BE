package com.example.noteservice.snapshot.query.response;

import lombok.Getter;

import java.util.function.Function;

@Getter
public class ValidSnapshotsResponse {

    private final String title;

    private String previewImageUrl;

    // 처음엔 ImageKey 를 입력받는다
    public ValidSnapshotsResponse(final String title, final String previewImageKey) {
        this.title = title;
        this.previewImageUrl = previewImageKey;
    }

    // 이후 입력받은 ImageKey 를 Presigned URL 로 변환시킨다.
    public void convertKeyToUrl(Function<String, String> converter) {
        this.previewImageUrl = converter.apply(previewImageUrl);
    }
}
