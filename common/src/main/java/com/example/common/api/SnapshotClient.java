package com.example.common.api;

public interface SnapshotClient {


    Long createSnapshot(CreateSnapshotRequest request);

    void deleteSnapshot(Long snapshotId);

}
