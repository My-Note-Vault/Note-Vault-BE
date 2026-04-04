package com.example.platformservice.member.ui;

import com.example.common.AuthMemberId;
import com.example.platformservice.member.application.MemberService;
import com.example.platformservice.member.ui.dto.CompleteProfileRequest;
import com.example.platformservice.member.ui.dto.CreateAccountRequest;
import com.example.platformservice.member.ui.dto.UpdateLastVisitedPathRequest;
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

    @PostMapping("/profile")
    public ResponseEntity<Void> completeProfile(
            @RequestBody final CompleteProfileRequest request,
            @AuthMemberId final Long memberId
    ) {
        memberService.completeProfile(request, memberId);
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
