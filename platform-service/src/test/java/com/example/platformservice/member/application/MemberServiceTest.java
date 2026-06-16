package com.example.platformservice.member.application;

import com.example.common.exception.UnauthorizedException;
import com.example.common.file.image.ImageUtils;
import com.example.platformservice.member.domain.Member;
import com.example.platformservice.member.domain.value.DayStartTime;
import com.example.platformservice.member.infra.MemberRepository;
import com.example.platformservice.member.ui.dto.CompleteProfileRequest;
import com.example.platformservice.member.ui.dto.MemberProfileResponse;
import com.example.platformservice.member.ui.dto.ProfileImageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.example.platformservice.PlatformConst.NO_USER_MESSAGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ImageUtils imageUtils;

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
            CompleteProfileRequest request = new CompleteProfileRequest("닉네임", 9, 30);
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

            // when
            memberService.completeProfile(request, memberId);

            // then
            assertThat(member.getNickname()).isEqualTo("닉네임");
            assertThat(member.getDayStartTime()).isEqualTo(new DayStartTime(9, 30));
            verify(memberRepository).save(member);
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 예외가 발생한다")
        void completeProfile_memberNotFound() {
            // given
            CompleteProfileRequest request = new CompleteProfileRequest("닉네임", 9, 30);
            given(memberRepository.findById(memberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.completeProfile(request, memberId))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage(NO_USER_MESSAGE);
        }
    }

    @Nested
    @DisplayName("getProfile 메소드는")
    class GetProfileTest {

        private Member member;
        private final Long memberId = 1L;

        @BeforeEach
        void setUp() {
            member = Member.googleSignUp("test@gmail.com", "테스트유저", "google123");
        }

        @Test
        @DisplayName("회원 프로필 정보를 반환한다")
        void getProfile_success() {
            member.completeProfile("닉네임", 9, 30);
            member.updateProfileImageKey("profile-image-key");
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

            MemberProfileResponse result = memberService.getProfile(memberId);

            assertThat(result.getNickname()).isEqualTo("닉네임");
            assertThat(result.getProfileImageKey()).isEqualTo("profile-image-key");
            assertThat(result.getName()).isEqualTo("테스트유저");
            assertThat(result.getDayStartTime()).isEqualTo(member.getDayStartTime());
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 예외가 발생한다")
        void getProfile_memberNotFound() {
            given(memberRepository.findById(memberId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> memberService.getProfile(memberId))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage(NO_USER_MESSAGE);
        }
    }

    @Nested
    @DisplayName("getProfileImage 메소드는")
    class GetProfileImageTest {

        private Member member;
        private final Long memberId = 1L;

        @BeforeEach
        void setUp() {
            member = Member.googleSignUp("test@gmail.com", "테스트유저", "google123");
        }

        @Test
        @DisplayName("프로필 이미지가 있으면 presigned 조회 URL과 key를 반환한다")
        void getProfileImage_success() {
            member.updateProfileImageKey("profile-image-key");
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            given(imageUtils.generatePresignedGetUrl("profile-image-key")).willReturn("https://signed-url");

            ProfileImageResponse result = memberService.getProfileImage(memberId);

            assertThat(result.getProfileImageKey()).isEqualTo("profile-image-key");
            assertThat(result.getProfileImageUrl()).isEqualTo("https://signed-url");
        }

        @Test
        @DisplayName("프로필 이미지가 없으면 빈 값을 반환한다")
        void getProfileImage_empty() {
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

            ProfileImageResponse result = memberService.getProfileImage(memberId);

            assertThat(result.getProfileImageKey()).isEmpty();
            assertThat(result.getProfileImageUrl()).isEmpty();
            verify(imageUtils, never()).generatePresignedGetUrl(anyString());
        }
    }

    @Nested
    @DisplayName("updateProfileImage 메소드는")
    class UpdateProfileImageTest {

        private Member member;
        private final Long memberId = 1L;

        @BeforeEach
        void setUp() {
            member = Member.googleSignUp("test@gmail.com", "테스트유저", "google123");
        }

        @Test
        @DisplayName("프로필 이미지 key를 성공적으로 변경한다")
        void updateProfileImage_success() {
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

            memberService.updateProfileImage(memberId, "new-profile-image-key");

            assertThat(member.getProfileImageKey()).isEqualTo("new-profile-image-key");
        }
    }

    @Nested
    @DisplayName("deleteProfileImage 메소드는")
    class DeleteProfileImageTest {

        private Member member;
        private final Long memberId = 1L;

        @BeforeEach
        void setUp() {
            member = Member.googleSignUp("test@gmail.com", "테스트유저", "google123");
        }

        @Test
        @DisplayName("프로필 이미지가 있으면 S3에서 삭제하고 key를 비운다")
        void deleteProfileImage_success() {
            member.updateProfileImageKey("profile-image-key");
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

            memberService.deleteProfileImage(memberId);

            verify(imageUtils).deleteImage("profile-image-key");
            assertThat(member.getProfileImageKey()).isEmpty();
        }

        @Test
        @DisplayName("프로필 이미지가 없으면 삭제를 시도하지 않는다")
        void deleteProfileImage_empty() {
            given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

            memberService.deleteProfileImage(memberId);

            verify(imageUtils, never()).deleteImage(any());
            assertThat(member.getProfileImageKey()).isEmpty();
        }
    }
}
