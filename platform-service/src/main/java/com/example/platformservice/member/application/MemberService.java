package com.example.platformservice.member.application;

import com.example.platformservice.member.domain.Account;
import com.example.platformservice.member.domain.Member;
import com.example.platformservice.member.domain.value.DayStartTime;
import com.example.platformservice.member.infra.AccountRepository;
import com.example.platformservice.member.infra.MemberRepository;
import com.example.platformservice.member.ui.dto.CompleteProfileRequest;
import com.example.platformservice.member.ui.dto.CreateAccountRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

import static com.example.platformservice.Const.NO_USER_MESSAGE;

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
                .orElseThrow(() -> new NoSuchElementException(NO_USER_MESSAGE));

        member.completeProfile(request.getNickname(), request.getProfileImageUrl());
    }

    @Transactional(readOnly = true)
    public List<Long> findAllMembersByDayStartTime(final DayStartTime dayStartTime) {
        List<Member> members = memberRepository.findAllByDayStartTime(dayStartTime);

        return members.stream()
                .map(Member::getId)
                .toList();
    }

    @Transactional(readOnly = true)
    public String getLastVisitedPath(final Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException(NO_USER_MESSAGE));

        return member.getLastViewedPath();
    }

    @Transactional
    public void updateLastVisitedPath(final Long memberId, final String lastVisitedPath) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException(NO_USER_MESSAGE));

        member.setLastViewedPath(lastVisitedPath);
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
