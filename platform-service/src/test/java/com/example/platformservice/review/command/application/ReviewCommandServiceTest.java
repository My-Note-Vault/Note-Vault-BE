package com.example.platformservice.review.command.application;

import com.example.platformservice.review.command.domain.Review;
import com.example.platformservice.review.command.domain.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewCommandServiceTest {

    @InjectMocks
    private ReviewCommandService reviewCommandService;

    @Mock
    private ReviewRepository reviewRepository;

    @Nested
    @DisplayName("createReview 메소드는")
    class CreateReviewTest {

        private final Long noteInfoId = 100L;
        private final Long reviewerId = 1L;
        private final Integer rating = 5;
        private final String content = "좋은 노트입니다!";

        @Test
        @DisplayName("리뷰를 성공적으로 생성한다")
        void createReview_success() {
            // given
            given(reviewRepository.save(any(Review.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            reviewCommandService.createReview(noteInfoId, reviewerId, rating, content);

            // then
            ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
            verify(reviewRepository).save(reviewCaptor.capture());

            Review savedReview = reviewCaptor.getValue();
            assertThat(savedReview.getNoteInfoId()).isEqualTo(noteInfoId);
            assertThat(savedReview.getReviewerId()).isEqualTo(reviewerId);
            assertThat(savedReview.getRating()).isEqualTo(rating);
            assertThat(savedReview.getContent()).isEqualTo(content);
        }

        @Test
        @DisplayName("내용 없이 리뷰를 생성할 수 있다")
        void createReview_withoutContent() {
            // given
            given(reviewRepository.save(any(Review.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            reviewCommandService.createReview(noteInfoId, reviewerId, rating, "");

            // then
            ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
            verify(reviewRepository).save(reviewCaptor.capture());

            Review savedReview = reviewCaptor.getValue();
            assertThat(savedReview.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteReview 메소드는")
    class DeleteReviewTest {

        private final Long reviewId = 10L;
        private final Long reviewerId = 1L;

        @Test
        @DisplayName("작성자가 리뷰를 성공적으로 삭제한다")
        void deleteReview_success() throws Exception {
            // given
            Review review = new Review(reviewerId, 100L, 5, "테스트 리뷰");
            setId(review, reviewId);
            given(reviewRepository.findById(reviewerId)).willReturn(Optional.of(review));

            // when
            reviewCommandService.deleteReview(reviewId, reviewerId);

            // then
            verify(reviewRepository).deleteById(reviewId);
        }

        @Test
        @DisplayName("존재하지 않는 리뷰면 예외가 발생한다")
        void deleteReview_notFound() {
            // given
            given(reviewRepository.findById(reviewerId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewCommandService.deleteReview(reviewId, reviewerId))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("일치하는 노트가 없습니다");
        }

        @Test
        @DisplayName("작성자가 아니면 예외가 발생한다")
        void deleteReview_notAuthor() throws Exception {
            // given
            Long anotherReviewerId = 2L;
            Review review = new Review(anotherReviewerId, 100L, 5, "테스트 리뷰");
            setId(review, reviewId);
            given(reviewRepository.findById(reviewerId)).willReturn(Optional.of(review));

            // when & then
            assertThatThrownBy(() -> reviewCommandService.deleteReview(reviewId, reviewerId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("작성자만 삭제할 수 있습니다");
        }

        private void setId(Review review, Long id) throws Exception {
            Field idField = Review.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(review, id);
        }
    }
}
