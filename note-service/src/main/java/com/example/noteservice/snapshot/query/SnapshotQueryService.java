package com.example.noteservice.snapshot.query;

import com.example.common.file.ImageUtils;
import com.example.noteservice.snapshot.command.domain.SnapshotRepository;
import com.example.noteservice.snapshot.query.response.ValidSnapshotsResponse;
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
