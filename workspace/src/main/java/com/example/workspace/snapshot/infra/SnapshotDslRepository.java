package com.example.workspace.snapshot.infra;

import com.example.workspace.snapshot.command.domain.Snapshot;
import com.example.workspace.snapshot.query.response.ValidSnapshotsResponse;

import java.util.List;

public interface SnapshotDslRepository {
    List<ValidSnapshotsResponse> findAllValidSnapshots(Long memberId);

    Snapshot findLatestSnapshot(Long noteId);
}
