package com.example.platformservice.member.application;

import com.example.common.exception.UnauthorizedException;
import com.example.common.file.image.ImageUtils;
import com.example.common.file.image.UploadImageResponse;
import com.example.platformservice.member.domain.Member;
import com.example.platformservice.member.domain.PayoutAccountVerification;
import com.example.platformservice.member.domain.value.DayStartTime;
import com.example.platformservice.member.infra.MemberRepository;
import com.example.platformservice.member.infra.PayoutAccountVerificationRepository;
import com.example.platformservice.member.ui.dto.CompleteProfileRequest;
import com.example.platformservice.member.ui.dto.GenerateProfileImageUploadUrlResponse;
import com.example.platformservice.member.ui.dto.MemberProfileResponse;
import com.example.platformservice.member.ui.dto.ProfileImageResponse;
import com.example.platformservice.member.ui.dto.PayoutAccountResponse;
import com.example.platformservice.member.ui.dto.UpdatePayoutAccountRequest;
import com.example.platformservice.member.ui.dto.SavePayoutAccountRequest;
import com.example.platformservice.member.ui.dto.PayoutAccountVerificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.platformservice.PlatformConst.NO_USER_MESSAGE;

@RequiredArgsConstructor
@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final ImageUtils imageUtils;
    private final PayoutAccountVerificationService payoutAccountVerificationService;
    private final PayoutAccountVerificationRepository payoutAccountVerificationRepository;

    @Transactional
    public void completeProfile(
            final CompleteProfileRequest request,
            final Long memberId
    ) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new UnauthorizedException(NO_USER_MESSAGE));

        member.completeProfile(request.getNickname(), request.getDayStartHour(), request.getDayStartMinute());
        memberRepository.save(member);
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
                .orElseThrow(() -> new UnauthorizedException(NO_USER_MESSAGE));

        return member.getLastVisitedPath();
    }

    @Transactional
    public void updateLastVisitedPath(final Long memberId, final String lastVisitedPath) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new UnauthorizedException(NO_USER_MESSAGE));

        member.setLastVisitedPath(lastVisitedPath);
    }

    @Transactional(readOnly = true)
    public MemberProfileResponse getProfile(final Long memberId) {
        Member member = findMember(memberId);

        return new MemberProfileResponse(
                member.getName(),
                member.getNickname(),
                member.getMemberTag(),
                member.getProfileImageKey(),
                member.getDayStartTime(),
                payoutAccountResponse(member)
        );
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
        String profileImageKey = getProfileImageKey(memberId);

        if (profileImageKey == null || profileImageKey.isBlank()) {
            return new ProfileImageResponse("", "");
        }

        return new ProfileImageResponse(
                imageUtils.generatePresignedGetUrl(profileImageKey),
                profileImageKey
        );
    }

    @Transactional(readOnly = true)
    public String getProfileImageKey(final Long memberId) {
        Member member = findMember(memberId);
        return member.getProfileImageKey();
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

    @Transactional(readOnly = true)
    public PayoutAccountResponse getPayoutAccount(final Long memberId) {
        Member member = findMember(memberId);
        return payoutAccountResponse(member);
    }

    private PayoutAccountResponse payoutAccountResponse(final Member member) {
        if (!member.hasPayoutAccount()) {
            return PayoutAccountResponse.empty();
        }

        return new PayoutAccountResponse(
                true,
                member.hasVerifiedPayoutAccount(),
                member.getPayoutBankCode(),
                member.getPayoutBankCode().getName(),
                maskAccountNumber(member.getPayoutAccountNumber()),
                maskHolderName(member.getPayoutAccountHolderName())
        );
    }

    public PayoutAccountVerificationResponse verifyPayoutAccount(
            final Long memberId,
            final UpdatePayoutAccountRequest request
    ) {
        validateMemberExists(memberId);
        String normalizedAccountNumber = request.getAccountNumber().replace("-", "");
        if (normalizedAccountNumber.length() < 6 || normalizedAccountNumber.length() > 14) {
            throw new IllegalArgumentException("계좌번호는 숫자 6~14자리여야 합니다");
        }

        PayoutAccountVerificationService.VerifiedAccount verifiedAccount =
                payoutAccountVerificationService.verify(request.getBankCode(), normalizedAccountNumber);
        PayoutAccountVerification verification = payoutAccountVerificationRepository.save(
                PayoutAccountVerification.create(
                        memberId,
                        request.getBankCode(),
                        normalizedAccountNumber,
                        verifiedAccount.holderName()
                )
        );

        return new PayoutAccountVerificationResponse(
                verification.getToken(),
                verification.getBankCode().getName(),
                maskAccountNumber(verification.getAccountNumber()),
                maskHolderName(verification.getHolderName()),
                verification.getExpiresAt()
        );
    }

    @Transactional
    public void saveVerifiedPayoutAccount(final Long memberId, final SavePayoutAccountRequest request) {
        PayoutAccountVerification verification = payoutAccountVerificationRepository
                .findByTokenAndMemberId(request.getVerificationToken(), memberId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 계좌 인증입니다"));
        verification.consume();

        Member member = findMember(memberId);
        member.updateVerifiedPayoutAccount(
                verification.getBankCode(),
                verification.getAccountNumber(),
                verification.getHolderName()
        );
    }

    @Transactional
    public void deletePayoutAccount(final Long memberId) {
        Member member = findMember(memberId);
        member.deletePayoutAccount();
    }

    private String maskAccountNumber(final String accountNumber) {
        if (accountNumber.length() <= 4) {
            return "*".repeat(accountNumber.length());
        }
        return "*".repeat(accountNumber.length() - 4)
                + accountNumber.substring(accountNumber.length() - 4);
    }

    private String maskHolderName(final String holderName) {
        if (holderName == null || holderName.isBlank()) {
            return null;
        }
        if (holderName.length() == 1) {
            return holderName;
        }
        if (holderName.length() == 2) {
            return holderName.charAt(0) + "*";
        }
        return holderName.charAt(0)
                + "*".repeat(holderName.length() - 2)
                + holderName.charAt(holderName.length() - 1);
    }

    private void validateMemberExists(final Long memberId) {
        findMember(memberId);
    }

    private Member findMember(final Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new UnauthorizedException(NO_USER_MESSAGE));
    }


}
