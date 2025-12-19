package com.example.platformservice.member.application;

import com.example.platformservice.member.domain.Account;
import com.example.platformservice.member.domain.Member;
import com.example.platformservice.member.domain.value.BankCode;
import com.example.platformservice.member.infra.AccountRepository;
import com.example.platformservice.member.infra.MemberRepository;
import com.example.platformservice.member.ui.dto.CompleteProfileRequest;
import com.example.platformservice.member.ui.dto.CreateAccountRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AccountRepository accountRepository;

    @Nested
    @DisplayName("completeProfile 메소드는")
    class CompleteProfileTest {

        private Member member;
        private final Long memberId = 1L;

        @BeforeEach
        void setUp() {
            member = Member.googleSignUp("test@gmail.com", "테스트유저", "google123");
        }

        @Test
        @DisplayName("프로필을 성공적으로 완성한다")
        void completeProfile_success() {
            // given
            CompleteProfileRequest request = new CompleteProfileRequest("닉네임", "profile-image-key");
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

            // when
            memberService.completeProfile(request, memberId);

            // then
            assertThat(member.getNickname()).isEqualTo("닉네임");
            assertThat(member.getProfileImageKey()).isEqualTo("profile-image-key");
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 예외가 발생한다")
        void completeProfile_memberNotFound() {
            // given
            CompleteProfileRequest request = new CompleteProfileRequest("닉네임", "profile-image-key");
            given(memberRepository.findById(memberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.completeProfile(request, memberId))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("일치하는 User 가 없습니다!");
        }
    }

    @Nested
    @DisplayName("addAccountInformation 메소드는")
    class AddAccountInformationTest {

        private Member member;
        private final Long memberId = 1L;

        @BeforeEach
        void setUp() {
            member = Member.googleSignUp("test@gmail.com", "테스트유저", "google123");
        }

        @Test
        @DisplayName("계좌 정보를 성공적으로 추가한다")
        void addAccountInformation_success() {
            // given
            CreateAccountRequest request = new CreateAccountRequest(
                    BankCode.KAKAO,
                    "1234567890",
                    "홍길동"
            );
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(accountRepository.save(any(Account.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            memberService.addAccountInformation(request, memberId);

            // then
            ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(accountCaptor.capture());

            Account savedAccount = accountCaptor.getValue();
            assertThat(savedAccount.getBankCode()).isEqualTo(BankCode.KAKAO);
            assertThat(savedAccount.getAccountNumber()).isEqualTo("1234567890");
            assertThat(savedAccount.getAccountHolderName()).isEqualTo("홍길동");
            assertThat(savedAccount.getMember()).isEqualTo(member);
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 예외가 발생한다")
        void addAccountInformation_memberNotFound() {
            // given
            CreateAccountRequest request = new CreateAccountRequest(
                    BankCode.KAKAO,
                    "1234567890",
                    "홍길동"
            );
            given(memberRepository.findById(memberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.addAccountInformation(request, memberId))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("일치하는 User 가 없습니다!");
        }
    }
}
