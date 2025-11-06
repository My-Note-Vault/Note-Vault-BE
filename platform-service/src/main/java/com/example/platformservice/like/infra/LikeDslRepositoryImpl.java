package com.example.platformservice.like.infra;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import static com.example.platformservice.like.domain.QLike.like;

@RequiredArgsConstructor
@Repository
public class LikeDslRepositoryImpl implements LikeDslRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Long countByNoteInfoId(final Long noteInfoId){
        return queryFactory.select(like.count())
                .from(like)
                .where(like.noteInfoId.eq(noteInfoId))
                .fetchOne();
    }
}
