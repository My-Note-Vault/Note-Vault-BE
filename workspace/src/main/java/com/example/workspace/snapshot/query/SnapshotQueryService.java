package com.example.workspace.snapshot.query;

import com.example.common.file.image.ImageUtils;
import com.example.workspace.snapshot.command.domain.SnapshotRepository;
import com.example.workspace.snapshot.query.response.ValidSnapshotsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class SnapshotQueryService {

    private final SnapshotRepository snapshotRepository;
    private final ImageUtils imageUtils;

    public List<ValidSnapshotsResponse> findAllValidSnapshot(final Long memberId) {
        List<ValidSnapshotsResponse> responses = snapshotRepository.findAllValidSnapshots(memberId);

        responses.forEach(response -> {
                    response.convertKeyToUrl(imageUtils::generatePresignedGetUrl);
                });

        return responses;
    }
}
