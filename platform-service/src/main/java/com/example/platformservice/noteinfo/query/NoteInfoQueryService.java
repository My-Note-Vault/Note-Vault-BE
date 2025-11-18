package com.example.platformservice.noteinfo.query;

import com.example.common.api.SnapshotDetailResponse;
import com.example.common.api.NoteReader;
import com.example.common.file.Image;
import com.example.common.file.ImageUtils;
import com.example.common.file.UploadStatus;
import com.example.platformservice.noteinfo.command.domain.NoteInfo;
import com.example.platformservice.noteinfo.command.domain.NoteInfoCategory;
import com.example.platformservice.noteinfo.command.domain.NoteInfoRepository;

import com.example.platformservice.noteinfo.command.domain.value.Category;
import com.example.platformservice.noteinfo.command.domain.value.Status;
import com.example.platformservice.noteinfo.query.response.InfoSummaryResponse;
import com.example.platformservice.noteinfo.query.response.MyNoteInfoSummaryResponse;
import com.example.platformservice.noteinfo.query.response.NoteInfoDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class NoteInfoQueryService {

    private final NoteInfoRepository noteInfoRepository;
    private final NoteReader noteReader;

    private final ImageUtils imageUtils;

    // '자세히' 조회하는 경우다.. 굳이 여기서 원본이 보여야 할 필요가 있는가
    // 원본이 보일 필요가 없다면 대체 어느 경우에 원본이 보이는거지?
    // PUBLIC, PRIVATE 의 경우에는 원본이 보이는 것이 좋을듯
    // FOR_SALE 인 경우에만 '스냅샷'을 보여주는거야
    // 그러면 원본과 스냅샷 각각을 저장하고 있어야 겠네???

    //TODO: 원본 글도 같이 보여야 한다 (형식 맞춰서 리턴할 것)
    // 호출 후 ImageUrl 얻는 호출도 해야한다.
    // findLatest10ReviewsWithAverageRatingAndTotalCount() 호출 필요
    // LikeCount 도 호출해야한다
    @Transactional(readOnly = true)
    public NoteInfoDetailResponse findNoteInfoById(final Long noteInfoId) {
        NoteInfo noteInfo = noteInfoRepository.findByIdAndStatusNot(noteInfoId, Status.PRIVATE)
                .orElseThrow(() -> new NoSuchElementException("No such NoteInfo"));

        List<String> infoImageKeys = noteInfo.getImages().stream()
                .filter(image -> image.getUploadStatus() == UploadStatus.CONFIRMED)
                .map(Image::getKey)
                .toList();

        List<Category> categories = noteInfo.getCategories().stream()
                .map(NoteInfoCategory::getCategory)
                .toList();

        SnapshotDetailResponse snapshotDetail = noteReader.getSnapshotDetail(noteInfo.getSnapshotId());
        String snapshotImageKey = snapshotDetail.getPreviewImageKey();
        if (noteInfo.getStatus().equals(Status.FOR_SALE)) {
            snapshotImageKey = snapshotDetail.getBlurredImageKey();
        }

        return NoteInfoDetailResponse.builder()
                .infoId(noteInfoId)
                .authorId(noteInfo.getAuthorId())
                .infoTitle(noteInfo.getTitle())
                .infoDescription(noteInfo.getDescription())
                .infoImageKeys(infoImageKeys)
                .price(noteInfo.getPrice())
                .categories(categories)
                .snapshotId(snapshotDetail.getSnapshotId())
                .snapshotImageKey(snapshotImageKey)
                .build();
    }

    @Transactional(readOnly = true)
    public NoteInfoDetailResponse findBlurredNoteInfoById(final Long noteInfoId) {
        return null;
    }


    //TODO: 내가 작성한 노트 관련 게시글 목록 조회..
    // 이것 역시 Description 이 너무 큰 경우 힘들텐데.. 필요한 값만 조회하는 것이 좋지 않을까..
    @Transactional(readOnly = true)
    public List<MyNoteInfoSummaryResponse> findMyAllNoteInfos(final Long memberId) {
        return null;
    }

    /**
     * 전체 목록 조회
     */
    @Transactional(readOnly = true)
    public List<InfoSummaryResponse> findInfoSummaries(int page) {
        return noteInfoRepository.findPagedInfoSummaries(page);
    }

    /**
     * 카테고리별 목록 조회
     */
    @Transactional(readOnly = true)
    public List<InfoSummaryResponse> findInfoSummariesByCategory(
            final Category category,
            final int page
    ) {
        return noteInfoRepository.findPagedInfoSummariesByCategory(category, page);
    }

    /**
     * 카테고리별 top5 목록 조회
     */
    @Transactional(readOnly = true)
    public List<InfoSummaryResponse> findBestInfoSummariesByCategory(final Category category) {
        return noteInfoRepository.findBestInfoSummariesByCategory(category);
    }

    /**
     * 현재 카테고리 && 글쓴이의 top5 목록 조회
     */
    @Transactional(readOnly = true)
    public List<InfoSummaryResponse> findHisBestInfoSummariesByCategory(
            final Long authorId,
            final Category category
    ) {
        return noteInfoRepository.findHisBestInfoSummariesByCategory(authorId, category);
    }
}
