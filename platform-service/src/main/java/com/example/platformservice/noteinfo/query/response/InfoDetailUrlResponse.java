package com.example.platformservice.noteinfo.query.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class InfoDetailUrlResponse {

    private final String snapshotImageUrl;
    private final List<String> imageUrls;
}
