package com.example.platformservice.review.ui;

import com.example.common.file.ImageUtils;
import com.example.platformservice.review.command.application.ReviewCommandService;
import com.example.platformservice.review.command.application.request.DeleteReviewRequest;
import com.example.platformservice.review.command.application.request.ReviewerImageKeysRequest;
import com.example.platformservice.review.command.application.request.WriteReviewRequest;
import com.example.platformservice.review.query.ReviewQueryService;
import com.example.platformservice.review.query.response.ReviewerImageKeyResponse;
import com.example.platformservice.review.query.response.Latest10ReviewResponse;
import com.example.platformservice.review.query.response.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/v1/review")
@RestController
public class ReviewController {

    private final ReviewCommandService reviewCommandService;
    private final ReviewQueryService reviewQueryService;

    private final ImageUtils imageUtils;


    @PostMapping
    public ResponseEntity<Long> writeReview(
            @RequestBody final WriteReviewRequest request,
            @RequestHeader("X-Member-Id") final Long memberId
    ) {
        Long reviewId = reviewCommandService.createReview(
                request.getNoteInfoId(),
                memberId,
                request.getRating(),
                request.getContent()
        );
        return ResponseEntity.ok(reviewId);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteReview(
            @RequestBody final DeleteReviewRequest request,
            @RequestHeader("X-Member-Id") final Long memberId
    ) {
        reviewCommandService.deleteReview(request.getReviewId(), memberId);
        return ResponseEntity.noContent().build();
    }


    /**
     * NoteInfo 조회 시 보여줄 Review 목록
     * Average Rating 및 Total Count 또한 보여준다
     */
    @GetMapping("/{noteInfoId}")
    public ResponseEntity<Latest10ReviewResponse> findLatest10ReviewsWithAverageRatingAndTotalCount(
            @PathVariable("noteInfoId") final Long noteInfoId
    ) {
        Latest10ReviewResponse latest10Reviews = reviewQueryService.findLatest10Reviews(noteInfoId);
        return ResponseEntity.ok(latest10Reviews);
    }


    /**
     * 이 메서드를 사용할 경우 아래의 `findTotalPageCount` 를 1번은 호출해야 한다.
     * (client 는 전체 page 개수를 알아야 함)
     */
    @GetMapping("/{noteInfoId}/{page}")
    public ResponseEntity<List<ReviewResponse>> findAllReviewsByNoteInfoId(
            @PathVariable("noteInfoId") final Long noteInfoId,
            @PathVariable("page") final int page
    ) {
        List<ReviewResponse> pagedReviews = reviewQueryService.findAllReviewsByNoteInfoId(noteInfoId, page);
        return ResponseEntity.ok(pagedReviews);
    }

    @GetMapping("/{noteInfoId}/total")
    public ResponseEntity<Long> findTotalPageCount(
            @PathVariable("noteInfoId") final Long noteInfoId
    ) {
        Long totalPageCount = reviewQueryService.findTotalPageCount(noteInfoId);
        return ResponseEntity.ok(totalPageCount);
    }


    /**
     * findLatest10ReviewsWithAverageRatingAndTotalCount() 호출 시 무조건 같이 호출해주어야 한다.
     * findAllReviewsByNoteInfoId() 호출 시 무조건 같이 호출해주어야 한다.
     * @param request ReviewerImageKeys
     * @return PresignedGetUrls..
     */
    @PostMapping("/image/reviewer")
    public ResponseEntity<ReviewerImageKeyResponse> convertReviewerImageKeysToImageUrl(
            @RequestBody final ReviewerImageKeysRequest request
    ) {
        List<String> imageUrls = request.getReviewerImageKeys().stream()
                .map(imageUtils::generatePresignedGetUrl)
                .toList();

        return ResponseEntity.ok(new ReviewerImageKeyResponse(imageUrls));
    }

}
