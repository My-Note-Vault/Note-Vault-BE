package com.example.platformservice.member.ui;

import com.example.common.AuthMemberId;
import com.example.common.file.cloudfront.CloudFrontCookieService;
import com.example.common.file.cloudfront.CloudFrontSignedCookie;
import com.example.platformservice.member.application.MemberService;
import com.example.platformservice.member.ui.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
@RestController
public class MemberController {

    private final MemberService memberService;
    private final CloudFrontCookieService cloudFrontCookieService;

    @GetMapping("/last-visited-path")
    public ResponseEntity<String> getLastVisitedPath(@AuthMemberId final Long memberId) {
        String lastVisitedPath = memberService.getLastVisitedPath(memberId);
        return ResponseEntity.ok(lastVisitedPath);
    }

    @PutMapping("/last-visited-path")
    public ResponseEntity<Void> updateLastVisitedPath(
            @RequestBody final UpdateLastVisitedPathRequest request,
            @AuthMemberId final Long memberId
    ) {
        memberService.updateLastVisitedPath(memberId, request.getLastVisitedPath());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/profile")
    public ResponseEntity<MemberProfileResponse> getProfile(@AuthMemberId final Long memberId) {
        MemberProfileResponse response = memberService.getProfile(memberId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/profile")
    public ResponseEntity<Void> completeProfile(
            @RequestBody final CompleteProfileRequest request,
            @AuthMemberId final Long memberId
    ) {
        memberService.completeProfile(request, memberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/payout-account")
    public ResponseEntity<PayoutAccountResponse> getPayoutAccount(
            @AuthMemberId final Long memberId
    ) {
        return ResponseEntity.ok(memberService.getPayoutAccount(memberId));
    }

    @PostMapping("/payout-account/verifications")
    public ResponseEntity<PayoutAccountVerificationResponse> verifyPayoutAccount(
            @Valid @RequestBody final UpdatePayoutAccountRequest request,
            @AuthMemberId final Long memberId
    ) {
        return ResponseEntity.ok(memberService.verifyPayoutAccount(memberId, request));
    }

    @PutMapping("/payout-account")
    public ResponseEntity<Void> saveVerifiedPayoutAccount(
            @Valid @RequestBody final SavePayoutAccountRequest request,
            @AuthMemberId final Long memberId
    ) {
        memberService.saveVerifiedPayoutAccount(memberId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/payout-account")
    public ResponseEntity<Void> deletePayoutAccount(@AuthMemberId final Long memberId) {
        memberService.deletePayoutAccount(memberId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/profile-image/upload-url")
    public ResponseEntity<GenerateProfileImageUploadUrlResponse> generateProfileImageUploadUrl(
            @Valid @RequestBody final GenerateProfileImageUploadUrlRequest request,
            @AuthMemberId final Long memberId
    ) {
        GenerateProfileImageUploadUrlResponse response =
                memberService.generateProfileImageUploadUrl(memberId, request.getContentType());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile-image")
    public ResponseEntity<ProfileImageResponse> getProfileImage(@AuthMemberId final Long memberId) {
        String profileImageKey = memberService.getProfileImageKey(memberId);
        if (profileImageKey == null || profileImageKey.isBlank()) {
            return ResponseEntity.ok(new ProfileImageResponse("", ""));
        }

        if (cloudFrontCookieService.isEnabled()) {
            CloudFrontSignedCookie signedCookie = cloudFrontCookieService.createSignedCookie(profileImageKey);

            ResponseEntity.BodyBuilder response = ResponseEntity.ok();
            signedCookie.getCookies()
                    .forEach(cookie -> response.header(HttpHeaders.SET_COOKIE, cookie.toString()));

            return response.body(new ProfileImageResponse(
                    signedCookie.getImageUrl(),
                    profileImageKey
            ));
        }

        return ResponseEntity.ok(memberService.getProfileImage(memberId));
    }

    @PatchMapping("/profile-image")
    public ResponseEntity<Void> updateProfileImage(
            @Valid @RequestBody final UpdateProfileImageRequest request,
            @AuthMemberId final Long memberId
    ) {
        memberService.updateProfileImage(memberId, request.getProfileImageKey());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/profile-image")
    public ResponseEntity<Void> deleteProfileImage(@AuthMemberId final Long memberId) {
        memberService.deleteProfileImage(memberId);
        return ResponseEntity.noContent().build();
    }

}
