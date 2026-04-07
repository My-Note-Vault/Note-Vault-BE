package com.example.platformservice.member.ui;

import com.example.common.AuthMemberId;
import com.example.platformservice.member.application.MemberService;
import com.example.platformservice.member.ui.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
@RestController
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/last-visited-path")
    public ResponseEntity<String> getLastVisitedPath(@AuthMemberId final Long memberId) {
        String lastVisitedPath = memberService.getLastVisitedPath(memberId);
        return ResponseEntity.ok(lastVisitedPath);
    }

    @PostMapping("/last-visited-path")
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
        ProfileImageResponse response = memberService.getProfileImage(memberId);
        return ResponseEntity.ok(response);
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

    @PostMapping
    public ResponseEntity<Long> createAccountForSelling(
            @RequestBody final CreateAccountRequest request,
            @AuthMemberId final Long memberId
    ) {
        Long accountId = memberService.addAccountInformation(request, memberId);
        return ResponseEntity.ok(accountId);
    }

}
