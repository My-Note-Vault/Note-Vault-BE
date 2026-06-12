package com.example.common.file.image;

import com.example.common.AuthMemberId;
import com.example.common.file.cloudfront.CloudFrontCookieService;
import com.example.common.file.cloudfront.CloudFrontSignedCookie;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/v1/content-images")
@RestController
public class ContentImageController {

    private final ImageUtils imageUtils;
    private final CloudFrontCookieService cloudFrontCookieService;

    @PostMapping("/upload-url")
    public ResponseEntity<GenerateContentImageUploadUrlResponse> generateUploadUrl(
            @RequestBody final GenerateContentImageUploadUrlRequest request,
            @AuthMemberId final Long memberId
    ) {
        if (request == null) {
            throw new IllegalArgumentException("요청 본문은 필수입니다.");
        }

        UploadImageResponse uploadImageResponse = imageUtils.generateContentImagePresignedPutUrl(
                memberId,
                ContentImageTarget.from(request.getTargetType()),
                request.getContentType()
        );

        return ResponseEntity.ok(new GenerateContentImageUploadUrlResponse(
                uploadImageResponse.getPresignedUrl(),
                uploadImageResponse.getKey()
        ));
    }

    @GetMapping("/cookies")
    public ResponseEntity<ContentImageAccessResponse> issueReadCookies(@AuthMemberId final Long memberId) {
        if (!cloudFrontCookieService.isEnabled()) {
            return ResponseEntity.ok(new ContentImageAccessResponse(false, "", 0));
        }

        CloudFrontSignedCookie signedCookie = cloudFrontCookieService.createSignedCookie();

        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        signedCookie.getCookies()
                .forEach(cookie -> response.header(HttpHeaders.SET_COOKIE, cookie.toString()));

        return response.body(new ContentImageAccessResponse(
                true,
                cloudFrontCookieService.baseUrl(),
                cloudFrontCookieService.expirationSeconds()
        ));
    }

    @GetMapping("/url")
    public ResponseEntity<ContentImageUrlResponse> generateUrl(
            @RequestParam final String key,
            @AuthMemberId final Long memberId
    ) {
        if (cloudFrontCookieService.isEnabled()) {
            imageUtils.validateContentImageKey(key);
            CloudFrontSignedCookie signedCookie = cloudFrontCookieService.createSignedCookie(key);

            ResponseEntity.BodyBuilder response = ResponseEntity.ok();
            signedCookie.getCookies()
                    .forEach(cookie -> response.header(HttpHeaders.SET_COOKIE, cookie.toString()));

            return response.body(new ContentImageUrlResponse(
                    cloudFrontCookieService.imageUrl(key),
                    key
            ));
        }

        return ResponseEntity.ok(new ContentImageUrlResponse(
                imageUtils.generateContentImagePresignedGetUrl(key),
                key
        ));
    }
}
