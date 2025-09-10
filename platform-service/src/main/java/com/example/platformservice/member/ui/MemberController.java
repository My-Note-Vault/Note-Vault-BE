package com.example.platformservice.member.ui;

import com.example.platformservice.auth.LoginMember;
import com.example.platformservice.member.application.MemberService;
import com.example.platformservice.member.domain.Member;
import com.example.platformservice.member.ui.dto.CompleteProfileRequest;
import com.example.platformservice.member.ui.dto.CreateAccountRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
@RestController
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<Long> completeProfile(
            @RequestBody final CompleteProfileRequest request,
            @LoginMember final Member member
    ) {
        Long memberId = memberService.completeProfile(request, member.getId());
        return ResponseEntity.ok(memberId);
    }

    @PostMapping
    public ResponseEntity<Long> createAccountForSelling(
            @RequestBody final CreateAccountRequest request,
            @LoginMember final Member member
    ) {
        Long accountId = memberService.addAccountInformation(request, member.getId());
        return ResponseEntity.ok(accountId);
    }

}
