package com.example.platformservice.noteinfo.infra;

import com.example.platformservice.noteinfo.command.domain.value.Category;
import com.example.platformservice.noteinfo.query.response.InfoSummaryResponse;
import com.example.platformservice.noteinfo.query.response.NoteInfoDetailResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.platformservice.like.domain.QLike.like;
import static com.example.platformservice.member.domain.QMember.member;
import static com.example.platformservice.noteinfo.command.domain.QNoteInfo.noteInfo;
import static com.example.platformservice.review.command.domain.QReview.review;


@RequiredArgsConstructor
@Repository
public class NoteInfoDslRepositoryImpl implements NoteInfoDslRepository {

    private static final int SIZE = 40;
    private static final int BEST_SIZE = 5;

    private final JPAQueryFactory queryFactory;

    public NoteInfoDetailResponse findNoteInfoDetail(final Long noteInfoId) {
        return queryFactory.select(Projections.constructor(NoteInfoDetailResponse.class,
                        noteInfo.id,
                        noteInfo.title,
                        noteInfo.description,
                        noteInfo.images
                ))
                .from(noteInfo)
                .fetchFirst();
    }

    @Override
    public List<InfoSummaryResponse> findPagedInfoSummaries(int page) {
        return queryFactory.select(Projections.constructor(InfoSummaryResponse.class,
                        noteInfo.images.get(0),
                        noteInfo.title,

                        review.count(),
                        review.rating.avg(),

                        like.count(),

                        member.profileImageKey,
                        member.nickname
                ))
                .from(noteInfo)
                .join(review).on(noteInfo.id.eq(review.noteInfoId))
                .join(like).on(noteInfo.id.eq(like.noteInfoId))
                .join(member).on(noteInfo.authorId.eq(member.id))
                .offset((page - 1) * SIZE)
                .limit(SIZE)
                .orderBy(noteInfo.createdAt.desc())
                .fetch();
    }

    @Override
    public List<InfoSummaryResponse> findPagedInfoSummariesByCategory(Category category, int page) {
        return queryFactory.select(Projections.constructor(InfoSummaryResponse.class,
                        noteInfo.images.get(0),
                        noteInfo.title,

                        review.count(),
                        review.rating.avg(),

                        like.count(),

                        member.profileImageKey,
                        member.nickname
                ))
                .from(noteInfo)
                .join(review).on(noteInfo.id.eq(review.noteInfoId))
                .join(like).on(noteInfo.id.eq(like.noteInfoId))
                .join(member).on(noteInfo.authorId.eq(member.id))
                .where(noteInfo.categories.any().category.eq(category))
                .offset((page - 1) * SIZE)
                .limit(SIZE)
                .orderBy(noteInfo.createdAt.desc())
                .fetch();
    }

    @Override
    public List<InfoSummaryResponse> findBestInfoSummariesByCategory(Category category) {
        return queryFactory.select(Projections.constructor(InfoSummaryResponse.class,
                        noteInfo.images.get(0),
                        noteInfo.title,

                        review.count(),
                        review.rating.avg(),

                        like.count(),

                        member.profileImageKey,
                        member.nickname
                ))
                .from(noteInfo)
                .join(review).on(noteInfo.id.eq(review.noteInfoId))
                .join(like).on(noteInfo.id.eq(like.noteInfoId))
                .join(member).on(noteInfo.authorId.eq(member.id))
                .where(noteInfo.categories.any().category.eq(category))
                .limit(BEST_SIZE)
                .orderBy(like.count().desc())
                .fetch();
    }

    @Override
    public List<InfoSummaryResponse> findHisBestInfoSummariesByCategory(Long authorId, Category category) {
        return queryFactory.select(Projections.constructor(InfoSummaryResponse.class,
                        noteInfo.images.get(0),
                        noteInfo.title,

                        review.count(),
                        review.rating.avg(),

                        like.count(),

                        member.profileImageKey,
                        member.nickname
                ))
                .from(noteInfo)
                .join(review).on(noteInfo.id.eq(review.noteInfoId))
                .join(like).on(noteInfo.id.eq(like.noteInfoId))
                .join(member).on(noteInfo.authorId.eq(member.id))
                .where(noteInfo.authorId.eq(authorId))
                .where(noteInfo.categories.any().category.eq(category))
                .limit(BEST_SIZE)
                .orderBy(like.count().desc())
                .fetch();
    }
}
