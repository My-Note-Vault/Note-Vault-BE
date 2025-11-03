package com.example.platformservice.review.query;

import com.example.platformservice.review.command.domain.ReviewRepository;
import com.example.platformservice.review.query.response.Latest10ReviewResponse;
import com.example.platformservice.review.query.response.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Review 는 어떻게 조회되어야 하는가..
 *
 * 단일 Review 조회는 필요하지 않다
 *
 * 전체 Review 가 조회되면 되는걸까
 * Review 도 페이징이 필요하다.
 *
 * 1. NoteInfo 조획 시 preview 로 상위 몇개만 보여주면 된다
 * 2. '더보기' 이후 페이징으로 보여주어야 한다
 */


@RequiredArgsConstructor
@Service
public class ReviewQueryService {

    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public Latest10ReviewResponse findLatest10Reviews(final Long noteInfoId) {
        return reviewRepository.findLatest10Reviews(noteInfoId);

    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> findAllReviewsByNoteInfoId(final Long noteInfoId, final int page) {
        return reviewRepository.findAllReviewsByNoteInfoId(noteInfoId, page);
    }

    @Transactional(readOnly = true)
    public Long findTotalPageCount(final Long noteInfoId) {
        return reviewRepository.findTotalPageCount(noteInfoId);
    }

}
