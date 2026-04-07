package com.example.platformservice.member.application;

import com.example.common.file.image.ImageUtils;
import com.example.common.file.image.UploadImageResponse;
import com.example.platformservice.member.domain.Account;
import com.example.platformservice.member.domain.Member;
import com.example.platformservice.member.domain.value.DayStartTime;
import com.example.platformservice.member.infra.AccountRepository;
import com.example.platformservice.member.infra.MemberRepository;
import com.example.platformservice.member.ui.dto.CompleteProfileRequest;
import com.example.platformservice.member.ui.dto.CreateAccountRequest;
import com.example.platformservice.member.ui.dto.GenerateProfileImageUploadUrlResponse;
import com.example.platformservice.member.ui.dto.MemberProfileResponse;
import com.example.platformservice.member.ui.dto.ProfileImageResponse;
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
    private final ImageUtils imageUtils;

    @Transactional
    public void completeProfile(
            final CompleteProfileRequest request,
            final Long memberId
    ) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException(NO_USER_MESSAGE));

        member.completeProfile(request.getNickname(), request.getProfileImageKey(), request.getDayStartTime());
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

    @Transactional(readOnly = true)
    public MemberProfileResponse getProfile(final Long memberId) {
        Member member = findMember(memberId);

        return new MemberProfileResponse(
                member.getNickname(),
                member.getProfileImageKey(),
                member.getDayStartTime()
        );
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

    @Transactional(readOnly = true)
    public GenerateProfileImageUploadUrlResponse generateProfileImageUploadUrl(
            final Long memberId,
            final String contentType
    ) {
        validateMemberExists(memberId);

        UploadImageResponse uploadImageResponse = imageUtils.generatePresignedPutUrl(memberId, contentType);
        return new GenerateProfileImageUploadUrlResponse(
                uploadImageResponse.getPresignedUrl(),
                uploadImageResponse.getKey()
        );
    }

    @Transactional(readOnly = true)
    public ProfileImageResponse getProfileImage(final Long memberId) {
        Member member = findMember(memberId);
        String profileImageKey = member.getProfileImageKey();

        if (profileImageKey == null || profileImageKey.isBlank()) {
            return new ProfileImageResponse("", "");
        }

        return new ProfileImageResponse(
                imageUtils.generatePresignedGetUrl(profileImageKey),
                profileImageKey
        );
    }

    @Transactional
    public void updateProfileImage(final Long memberId, final String profileImageKey) {
        Member member = findMember(memberId);
        String oldProfileImageKey = member.getProfileImageKey();

        if (oldProfileImageKey != null
                && !oldProfileImageKey.isBlank()
                && !oldProfileImageKey.equals(profileImageKey)) {
            imageUtils.deleteImage(oldProfileImageKey);
        }

        member.updateProfileImageKey(profileImageKey);
    }

    @Transactional
    public void deleteProfileImage(final Long memberId) {
        Member member = findMember(memberId);
        String profileImageKey = member.getProfileImageKey();

        if (profileImageKey == null || profileImageKey.isBlank()) {
            return;
        }

        imageUtils.deleteImage(profileImageKey);
        member.updateProfileImageKey("");
    }

    private void validateMemberExists(final Long memberId) {
        findMember(memberId);
    }

    private Member findMember(final Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException(NO_USER_MESSAGE));
    }


}
