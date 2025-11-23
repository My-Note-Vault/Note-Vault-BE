package com.example.noteservice.snapshot.command.application;

import com.example.common.api.CreateSnapshotRequest;
import com.example.noteservice.snapshot.command.domain.Snapshot;
import com.example.noteservice.snapshot.command.domain.SnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class SnapshotCommandService {

    private final SnapshotRepository snapshotRepository;

    @Transactional
    public Long createSnapshot(final CreateSnapshotRequest request) {
        Snapshot snapshot = Snapshot.builder()
                .noteId(request.getNoteId())
                .title(request.getTitle())
                .content(request.getContent())
                .previewImageKey(request.getPreviewImageKey())
                .blurredImageKey(request.getBlurredImageKey())
                .build();

        snapshotRepository.save(snapshot);
        return snapshot.getId();
    }

    @Transactional
    public void deleteSnapshot(final Long snapshotId) {
        // 이거 검증을 어떻게 하지? 여기서는 검증할 의무가 없다? 왜냐하면 Member 테이블 자체가 없으니까?
        snapshotRepository.deleteById(snapshotId);
    }
}
