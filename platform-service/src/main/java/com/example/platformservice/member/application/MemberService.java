package com.example.platformservice.member.application;

import com.example.platformservice.member.domain.Account;
import com.example.platformservice.member.domain.Member;
import com.example.platformservice.member.infra.AccountRepository;
import com.example.platformservice.member.infra.MemberRepository;
import com.example.platformservice.member.ui.dto.CompleteProfileRequest;
import com.example.platformservice.member.ui.dto.CreateAccountRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public void completeProfile(
            final CompleteProfileRequest request,
            final Long memberId
    ) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("일치하는 User 가 없습니다!"));

        member.completeProfile(request.getNickname(), request.getProfileImageUrl());
    }

    @Transactional
    public Long addAccountInformation(
            final CreateAccountRequest request,
            final Long memberId
    ) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("일치하는 User 가 없습니다!"));

        Account account = new Account(
                member,
                request.getBankCode(),
                request.getAccountNumber(),
                request.getAccountHolderName()
        );
        accountRepository.save(account);
        return account.getId();
    }


}
