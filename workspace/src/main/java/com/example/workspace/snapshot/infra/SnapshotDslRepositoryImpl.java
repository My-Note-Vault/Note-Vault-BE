package com.example.workspace.snapshot.infra;

import com.example.workspace.snapshot.command.domain.Snapshot;
import com.example.workspace.snapshot.query.response.ValidSnapshotsResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.workspace.snapshot.command.domain.QSnapshot.snapshot;

@RequiredArgsConstructor
@Repository
public class SnapshotDslRepositoryImpl implements SnapshotDslRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ValidSnapshotsResponse> findAllValidSnapshots(final Long memberId) {
        return queryFactory
                .select(
                        Projections.constructor(
                                ValidSnapshotsResponse.class,
                                snapshot.previewImageKey,
                                snapshot.title
                        )
                )
                .from(snapshot)
                .fetch();
    }

    @Override
    public Snapshot findLatestSnapshot(final Long noteId) {
        return queryFactory
                .select(snapshot)
                .from(snapshot)
                .where(snapshot.noteId.eq(noteId))
                .orderBy(snapshot.createdAt.desc())
                .limit(1)
                .fetchOne();
    }

}
