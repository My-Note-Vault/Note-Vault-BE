package com.example.workspace.snapshot.infra;

import com.example.common.api.CreateSnapshotRequest;
import com.example.common.api.SnapshotClient;
import com.example.workspace.snapshot.command.application.SnapshotCommandService;
import com.example.workspace.snapshot.query.SnapshotQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class MonolithSnapshotClient implements SnapshotClient {

    private final SnapshotCommandService snapshotCommandService;
    private final SnapshotQueryService snapshotQueryService;


    @Override
    public Long createSnapshot(CreateSnapshotRequest request) {
        return snapshotCommandService.createSnapshot(request);
    }

    @Override
    public void deleteSnapshot(Long snapshotId) {
        snapshotCommandService.deleteSnapshot(snapshotId);
    }
}
