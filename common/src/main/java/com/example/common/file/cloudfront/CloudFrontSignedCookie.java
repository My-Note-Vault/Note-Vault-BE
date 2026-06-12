package com.example.common.file.cloudfront;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class CloudFrontSignedCookie {

    private final String imageUrl;
    private final List<ResponseCookie> cookies;
}
