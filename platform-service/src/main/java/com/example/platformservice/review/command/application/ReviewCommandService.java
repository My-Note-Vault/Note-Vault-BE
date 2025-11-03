package com.example.platformservice.review.command.application;


import com.example.platformservice.review.command.domain.Review;
import com.example.platformservice.review.command.domain.ReviewRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class ReviewCommandService {

    private final ReviewRepository reviewRepository;

    @Transactional
    public Long createReview(
            final Long noteInfoId,
            final Long reviewerId,
            final Integer rating,
            final String content
    ) {
        Review review = new Review(reviewerId, noteInfoId, rating, content);
        reviewRepository.save(review);
        return review.getId();
    }

    @Transactional
    public void deleteReview(final Long reviewId, final Long reviewerId) {
        Review review = reviewRepository.findById(reviewerId)
                .orElseThrow(() -> new NoSuchElementException("일치하는 노트가 없습니다"));

        if (!review.getReviewerId().equals(reviewerId)) {
            throw new IllegalArgumentException("작성자만 삭제할 수 있습니다");
        }
        reviewRepository.deleteById(reviewId);
    }

}
