package com.example.noteservice.note.infra;

import com.example.common.api.NoteSummaryResponse;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.noteservice.note.command.domain.QNote.note;

@RequiredArgsConstructor
@Repository
public class NoteDslRepositoryImpl implements NoteDslRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<NoteSummaryResponse> findMyAllNotes(final Long memberId) {
        return queryFactory
                .select(getNoteSummaryProjection())
                .from(note)
                .where(note.authorId.eq(memberId))
                .fetch();
    }

    private ConstructorExpression<NoteSummaryResponse> getNoteSummaryProjection() {
        return Projections.constructor(
                NoteSummaryResponse.class,
                note.id,
                note.title
        );
    }
}
