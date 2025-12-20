package com.example.platformservice.like.application;

import com.example.platformservice.like.domain.Like;
import com.example.platformservice.like.domain.LikeRepository;
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
class LikeServiceTest {

    @InjectMocks
    private LikeService likeService;

    @Mock
    private LikeRepository likeRepository;

    @Nested
    @DisplayName("like 메소드는")
    class LikeTest {

        private final Long memberId = 1L;
        private final Long noteInfoId = 100L;

        @Test
        @DisplayName("좋아요를 성공적으로 추가한다")
        void like_success() {
            // given
            given(likeRepository.save(any(Like.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            likeService.like(memberId, noteInfoId);

            // then
            ArgumentCaptor<Like> likeCaptor = ArgumentCaptor.forClass(Like.class);
            verify(likeRepository).save(likeCaptor.capture());

            Like savedLike = likeCaptor.getValue();
            assertThat(savedLike.getMemberId()).isEqualTo(memberId);
            assertThat(savedLike.getNoteInfoId()).isEqualTo(noteInfoId);
        }
    }

    @Nested
    @DisplayName("deleteLike 메소드는")
    class DeleteLikeTest {

        private final Long memberId = 1L;
        private final Long noteInfoId = 100L;

        @Test
        @DisplayName("좋아요를 성공적으로 삭제한다")
        void deleteLike_success() {
            // given
            Like like = new Like(memberId, noteInfoId);
            given(likeRepository.findByNoteInfoIdAndMemberId(noteInfoId, memberId))
                    .willReturn(Optional.of(like));

            // when
            likeService.deleteLike(noteInfoId, memberId);

            // then
            verify(likeRepository).delete(like);
        }

        @Test
        @DisplayName("존재하지 않는 좋아요면 예외가 발생한다")
        void deleteLike_notFound() {
            // given
            given(likeRepository.findByNoteInfoIdAndMemberId(noteInfoId, memberId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> likeService.deleteLike(noteInfoId, memberId))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("좋아요가 존재하지 않습니다.");
        }
    }

    @Nested
    @DisplayName("countLikesByNoteInfoId 메소드는")
    class CountLikesTest {

        private final Long noteInfoId = 100L;

        @Test
        @DisplayName("좋아요 수를 반환한다")
        void countLikes_success() {
            // given
            given(likeRepository.countByNoteInfoId(noteInfoId)).willReturn(5L);

            // when
            Long count = likeService.countLikesByNoteInfoId(noteInfoId);

            // then
            assertThat(count).isEqualTo(5L);
        }

        @Test
        @DisplayName("좋아요가 없으면 0을 반환한다")
        void countLikes_zero() {
            // given
            given(likeRepository.countByNoteInfoId(noteInfoId)).willReturn(0L);

            // when
            Long count = likeService.countLikesByNoteInfoId(noteInfoId);

            // then
            assertThat(count).isEqualTo(0L);
        }
    }
}