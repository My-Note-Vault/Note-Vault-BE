package com.example.platformservice.review.infra;

import com.example.platformservice.review.query.response.Latest10ReviewResponse;
import com.example.platformservice.review.query.response.ReviewResponse;

import java.util.List;

public interface ReviewDslRepository {
    Latest10ReviewResponse findLatest10Reviews(Long noteInfoId);

    List<ReviewResponse> findAllReviewsByNoteInfoId(Long noteInfoId, int page);

    Long findTotalPageCount(Long noteInfoId);
}
