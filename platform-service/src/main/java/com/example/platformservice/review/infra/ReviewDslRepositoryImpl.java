package com.example.platformservice.review.infra;

import com.example.platformservice.review.query.response.Latest10ReviewResponse;
import com.example.platformservice.review.query.response.ReviewResponse;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.platformservice.member.domain.QMember.member;
import static com.example.platformservice.review.command.domain.QReview.review;

@RequiredArgsConstructor
@Repository
public class ReviewDslRepositoryImpl implements ReviewDslRepository {

    private static final Integer SIZE = 20; // 한 페이지에 보여주는 Reviews 양

    private final JPAQueryFactory queryFactory;

    @Override
    public Latest10ReviewResponse findLatest10Reviews(final Long noteInfoId) {
        List<ReviewResponse> reviewResponses = queryFactory.select(getReviewResponseProjection())
                .from(review)
                .where(review.noteInfoId.eq(noteInfoId))
                .orderBy(review.createdAt.desc())
                .limit(10)
                .fetch();

        Tuple tuple = queryFactory.select(review.count(), review.rating.avg())
                .from(review)
                .where(review.noteInfoId.eq(noteInfoId))
                .fetchOne();

        return Latest10ReviewResponse.builder()
                .totalReviewCount(tuple.get(review.count()))
                .averageRating(tuple.get(review.rating.avg()))
                .reviewResponses(reviewResponses)
                .build();
    }

    @Override
    public List<ReviewResponse> findAllReviewsByNoteInfoId(final Long noteInfoId, final int page) {
        return queryFactory.select(getReviewResponseProjection())
                .from(review)
                .where(review.noteInfoId.eq(noteInfoId))
                .orderBy(review.createdAt.desc())
                .offset((page - 1) * SIZE)
                .limit(SIZE)
                .fetch();
    }

    /**
     * 전체 페이지 개수가 필요함.. 근데 그냥 1..10 다음
     * 11 ~ 20 .. 다음 이렇게만 보여주면 된다
     * count 를 offset, limit 지정해서 하는 대신 한번에 다 해버리자
     */
    @Override
    public Long findTotalPageCount(final Long noteInfoId) {
        return queryFactory.select(review.count())
                .from(review)
                .where(review.noteInfoId.eq(noteInfoId))
                .fetchOne();
    }

    private ConstructorExpression<ReviewResponse> getReviewResponseProjection() {
        return Projections.constructor(ReviewResponse.class,
                review.id,
                review.reviewerId,
                member.nickname,
                member.profileImageUrl,
                review.content,
                review.rating,
                review.updatedAt
        );
    }
}
