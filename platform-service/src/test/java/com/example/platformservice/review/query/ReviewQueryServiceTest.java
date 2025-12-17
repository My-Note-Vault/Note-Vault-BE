package com.example.platformservice.review.query;

import com.example.platformservice.review.command.domain.ReviewRepository;
import com.example.platformservice.review.query.response.Latest10ReviewResponse;
import com.example.platformservice.review.query.response.ReviewResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReviewQueryServiceTest {

    @InjectMocks
    private ReviewQueryService reviewQueryService;

    @Mock
    private ReviewRepository reviewRepository;

    @Nested
    @DisplayName("findLatest10Reviews 메소드는")
    class FindLatest10ReviewsTest {

        private final Long noteInfoId = 100L;

        @Test
        @DisplayName("최신 10개 리뷰와 통계를 반환한다")
        void findLatest10Reviews_success() {
            // given
            List<ReviewResponse> reviews = List.of(
                    new ReviewResponse(1L, 1L, "유저1", "image1", "좋아요", 5, LocalDateTime.now()),
                    new ReviewResponse(2L, 2L, "유저2", "image2", "괜찮아요", 4, LocalDateTime.now())
            );
            Latest10ReviewResponse response = Latest10ReviewResponse.builder()
                    .totalReviewCount(2)
                    .averageRating(4.5)
                    .reviewResponses(reviews)
                    .build();

            given(reviewRepository.findLatest10Reviews(noteInfoId)).willReturn(response);

            // when
            Latest10ReviewResponse result = reviewQueryService.findLatest10Reviews(noteInfoId);

            // then
            assertThat(result.getTotalReviewCount()).isEqualTo(2);
            assertThat(result.getAverageRating()).isEqualTo(4.5);
            assertThat(result.getReviewResponses()).hasSize(2);
        }

        @Test
        @DisplayName("리뷰가 없으면 빈 리스트를 반환한다")
        void findLatest10Reviews_empty() {
            // given
            Latest10ReviewResponse response = Latest10ReviewResponse.builder()
                    .totalReviewCount(0)
                    .averageRating(0.0)
                    .reviewResponses(Collections.emptyList())
                    .build();

            given(reviewRepository.findLatest10Reviews(noteInfoId)).willReturn(response);

            // when
            Latest10ReviewResponse result = reviewQueryService.findLatest10Reviews(noteInfoId);

            // then
            assertThat(result.getTotalReviewCount()).isZero();
            assertThat(result.getReviewResponses()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllReviewsByNoteInfoId 메소드는")
    class FindAllReviewsTest {

        private final Long noteInfoId = 100L;
        private final int page = 0;

        @Test
        @DisplayName("페이징된 리뷰 목록을 반환한다")
        void findAllReviews_success() {
            // given
            List<ReviewResponse> reviews = List.of(
                    new ReviewResponse(1L, 1L, "유저1", "image1", "좋아요", 5, LocalDateTime.now()),
                    new ReviewResponse(2L, 2L, "유저2", "image2", "괜찮아요", 4, LocalDateTime.now())
            );

            given(reviewRepository.findAllReviewsByNoteInfoId(noteInfoId, page)).willReturn(reviews);

            // when
            List<ReviewResponse> result = reviewQueryService.findAllReviewsByNoteInfoId(noteInfoId, page);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getReviewerNickname()).isEqualTo("유저1");
        }
    }

    @Nested
    @DisplayName("findTotalPageCount 메소드는")
    class FindTotalPageCountTest {

        private final Long noteInfoId = 100L;

        @Test
        @DisplayName("전체 페이지 수를 반환한다")
        void findTotalPageCount_success() {
            // given
            given(reviewRepository.findTotalPageCount(noteInfoId)).willReturn(5L);

            // when
            Long result = reviewQueryService.findTotalPageCount(noteInfoId);

            // then
            assertThat(result).isEqualTo(5L);
        }

        @Test
        @DisplayName("리뷰가 없으면 0을 반환한다")
        void findTotalPageCount_zero() {
            // given
            given(reviewRepository.findTotalPageCount(noteInfoId)).willReturn(0L);

            // when
            Long result = reviewQueryService.findTotalPageCount(noteInfoId);

            // then
            assertThat(result).isZero();
        }
    }
}
