package com.example.platformservice.noteinfo.command.application;

import com.example.common.api.CreateSnapshotRequest;
import com.example.common.api.SnapshotClient;
import com.example.platformservice.noteinfo.command.application.request.CreateNoteInfoRequest;
import com.example.platformservice.noteinfo.command.application.request.UpdateNoteInfoWithoutImageRequest;
import com.example.platformservice.noteinfo.command.domain.NoteInfo;
import com.example.platformservice.noteinfo.command.domain.NoteInfoCategory;
import com.example.platformservice.noteinfo.command.domain.NoteInfoImage;
import com.example.platformservice.noteinfo.command.domain.NoteInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class NoteInfoCommandService {

    private final NoteInfoRepository noteInfoRepository;

    private final SnapshotClient snapshotClient;

    @Transactional
    public Long createNoteInfo(
            final CreateNoteInfoRequest request,
            final Long memberId,
            final String previewImageId,
            final String blurredImageId
    ) {
        Long snapshotId = createSnapshot(request, previewImageId, blurredImageId);
        return createNoteInfo(request, memberId, snapshotId);
    }

    private Long createNoteInfo(
            final CreateNoteInfoRequest request,
            final Long memberId,
            final Long snapshotId
    ) {
        List<NoteInfoCategory> categories = request.getCategories().stream()
                .map(NoteInfoCategory::new)
                .toList();

        NoteInfo noteInfo = NoteInfo.builder()
                .snapshotId(snapshotId)
                .authorId(memberId)
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .status(request.getStatus())
                .categories(categories)
                .build();

        noteInfoRepository.save(noteInfo);
        return noteInfo.getId();
    }

    private Long createSnapshot(
            final CreateNoteInfoRequest request,
            final String previewImageId,
            final String blurredImageId
    ) {
        CreateSnapshotRequest snapshotRequest = CreateSnapshotRequest.builder()
                .noteId(request.getNoteId())
                .previewImageKey(previewImageId)
                .blurredImageKey(blurredImageId)
                .title(request.getTitle())
                .content(request.getDescription())
                .build();

        return snapshotClient.createSnapshot(snapshotRequest);
    }


    @Transactional
    public void uploadImage(final Long noteInfoId, final String imageKey) {
        NoteInfo noteInfo = noteInfoRepository.findById(noteInfoId)
                .orElseThrow(() -> new NoSuchElementException("유효한 NoteInfo 가 없습니다"));

        NoteInfoImage noteInfoImage = new NoteInfoImage(imageKey);
        noteInfo.uploadImages(noteInfoImage);
    }


    @Transactional
    public void deleteNoteInfo(final Long noteInfoId, final Long memberId) {
        NoteInfo noteInfo = noteInfoRepository.findById(noteInfoId)
                .orElseThrow(() -> new NoSuchElementException("해당하는 NoteInfo 가 없습니다!"));

        if (!noteInfo.getAuthorId().equals(memberId)) {
            throw new IllegalArgumentException("작성자만 삭제할 수 있습니다!");
        }
        snapshotClient.deleteSnapshot(noteInfo.getSnapshotId());
        noteInfoRepository.delete(noteInfo);
    }

    @Transactional
    public void deleteImage(
            final Long noteInfoId,
            final Long memberId,
            final int imageIndex
    ) {
        NoteInfo noteInfo = noteInfoRepository.findByIdAndAuthorId(noteInfoId, memberId)
                .orElseThrow(() -> new NoSuchElementException("유효한 NoteInfo 가 없습니다"));

        noteInfo.getImages().remove(imageIndex);
    }


    @Transactional
    public void updateWithoutImage(final UpdateNoteInfoWithoutImageRequest request, final Long memberId) {
        NoteInfo noteInfo = noteInfoRepository.findByIdAndAuthorId(request.getNoteInfoId(), memberId)
                .orElseThrow(() -> new NoSuchElementException("유효한 NoteInfo 가 없습니다"));

        List<NoteInfoCategory> categories = request.getCategories().stream()
                .map(NoteInfoCategory::new)
                .toList();

        noteInfo.updateWithoutImage(
                request.getTitle(),
                request.getDescription(),
                request.getPrice(),
                request.getStatus(),
                categories
        );
    }

}
