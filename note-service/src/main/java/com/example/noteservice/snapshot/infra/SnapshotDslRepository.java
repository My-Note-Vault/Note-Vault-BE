package com.example.noteservice.snapshot.infra;

import com.example.noteservice.snapshot.command.domain.Snapshot;
import com.example.noteservice.snapshot.query.response.ValidSnapshotsResponse;

import java.util.List;

public interface SnapshotDslRepository {
    List<ValidSnapshotsResponse> findAllValidSnapshots(Long memberId);

    Snapshot findLatestSnapshot(Long noteId);
}
