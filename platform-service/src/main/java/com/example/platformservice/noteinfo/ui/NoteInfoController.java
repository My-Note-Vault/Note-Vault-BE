package com.example.platformservice.noteinfo.ui;

import com.example.common.file.ImageUtils;
import com.example.common.file.UploadImageResponse;
import com.example.platformservice.noteinfo.command.application.NoteInfoCommandService;
import com.example.platformservice.noteinfo.command.application.request.*;
import com.example.platformservice.noteinfo.command.domain.value.Category;
import com.example.platformservice.noteinfo.query.NoteInfoQueryService;
import com.example.platformservice.noteinfo.query.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/v1/note-info")
@RestController
public class NoteInfoController {

    private final NoteInfoCommandService noteInfoCommandService;
    private final NoteInfoQueryService noteInfoQueryService;

    private final ImageUtils imageUtils;


    // 여기는 Snapshot 을 위한 Image 는 있는데 왜 NoteInfo 를 위한 Images 는 없지
    // 현재 CreateNoteInfoRequest 에 List<String> imageKeys 가지고 있다..
    // 이걸 수정한다고 치면 몇 장을 올리는지 어떻게 알 수 있지??
    // 어차피 User 는 uploadImage 를 한 장씩 업로드하는구나
    @PostMapping
    public ResponseEntity<CreateNoteInfoResponse> createNoteInfo(
            @RequestBody final CreateNoteInfoRequest request,
            @RequestHeader("X-Member-Id") final Long memberId
    ) {
        UploadImageResponse uploadPreviewImageResponse = imageUtils.generatePresignedPutUrl(memberId);
        UploadImageResponse uploadBlurredImageResponse = imageUtils.generatePresignedPutUrl(memberId);

        Long noteInfoId = noteInfoCommandService.createNoteInfo(
                request,
                memberId,
                uploadPreviewImageResponse.getKey(),
                uploadBlurredImageResponse.getKey()
        );

        CreateNoteInfoResponse createNoteInfoResponse = new CreateNoteInfoResponse(
                noteInfoId,
                uploadPreviewImageResponse.getPresignedUrl(),
                uploadBlurredImageResponse.getPresignedUrl()
        );
        return ResponseEntity.ok(createNoteInfoResponse);
    }


    /**
     * 사진 여러장 올릴거면 이거 여러번 호출해라.
     */
    @PostMapping("/image")
    public ResponseEntity<String> uploadImage(
            @RequestBody final DeleteNoteInfoRequest request,
            @RequestHeader("X-Member-Id") final Long memberId
    ) {
        UploadImageResponse uploadImageResponse = imageUtils.generatePresignedPutUrl(memberId);
        noteInfoCommandService.uploadImage(request.getNoteInfoId(), uploadImageResponse.getKey());
        return ResponseEntity.ok(uploadImageResponse.getPresignedUrl()) ;
    }


    // Image 제외하고 수정
    @PatchMapping
    public ResponseEntity<Void> updateNoteInfoWithoutImage(
            @RequestBody final UpdateNoteInfoWithoutImageRequest request,
            @RequestHeader("X-Member-Id") final Long memberId
    ) {
        noteInfoCommandService.updateWithoutImage(request, memberId);
        return ResponseEntity.noContent().build();
    }


    @DeleteMapping
    public ResponseEntity<Void> deleteNoteInfo(
            @RequestBody final DeleteNoteInfoRequest request,
            @RequestHeader("X-Member-Id") final Long memberId
    ) {
        noteInfoCommandService.deleteNoteInfo(request.getNoteInfoId(), memberId);
        return ResponseEntity.noContent().build();
    }

    // NoteInfo 의 Image 수정
    @DeleteMapping("/image")
    public ResponseEntity<Void> deleteNoteInfoImage(
            @RequestBody final DeleteNoteInfoImageRequest request,
            @RequestHeader("X-Member-Id") final Long memberId
    ) {
        noteInfoCommandService.deleteImage(
                request.getNoteInfoId(),
                memberId,
                request.getImageIndex()
        );
        return ResponseEntity.noContent().build();
    }


    /**
     * PUBLIC 인 경우 preview 그대로 보여준다
     * FOR_SALE 인 경우 blurred 를 보여준다
     * PRIVATE 인 경우.. 조회가 되면 안되겠지
     *
     * 클라이언트는 여기서 Review 와 Like, Member 를 호출해야 한다
     * ImageUrl 로 변환하는 API 역시 호출해야한다.
     */
    @GetMapping("/{infoId}")
    public ResponseEntity<NoteInfoDetailResponse> findNoteInfoDetail(
            @PathVariable("infoId") final Long infoId
    ) {
        NoteInfoDetailResponse response = noteInfoQueryService.findNoteInfoById(infoId);
        return ResponseEntity.ok(response);
    }

    /**
     * findNoteInfoDetail() 호출 후 필수적으로 호출해야하는 ImageUrl 얻는 api
     * @param request: Image Key 들
     * @return PresignedGetUrl 로 변환된 값들
     */
    @PostMapping("/image/detail")
    public ResponseEntity<InfoDetailUrlResponse> convertDetailKeyToImageUrl(
            @RequestBody final InfoDetailKeyRequest request
    ) {
        String snapshotImageUrl = imageUtils.generatePresignedGetUrl(request.getSnapshotImageKey());
        List<String> imageUrls = request.getInfoImageKeys().stream()
                .map(imageUtils::generatePresignedGetUrl)
                .toList();

        InfoDetailUrlResponse response = new InfoDetailUrlResponse(snapshotImageUrl, imageUrls);
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 목록 최신순 조회
     * ImageUrl 로 변환하는 API 를 호출해야 한다 (convertSummariesKeyToUrl())
     */
    @GetMapping("/{page}")
    public ResponseEntity<List<InfoSummaryResponse>> findNoteInfoSummaries(
            @PathVariable("page") final int page
    ) {
        List<InfoSummaryResponse> responses = noteInfoQueryService.findInfoSummaries(page);
        return ResponseEntity.ok(responses);
    }

    /**
     * Category 별 최신순 조회
     * @param category RESUME, COVER_LETTER, OTHER
     */
    @GetMapping("/{category}/{page}")
    public ResponseEntity<List<InfoSummaryResponse>> findNoteInfoSummariesByCategory(
            @PathVariable("category") final Category category,
            @PathVariable("page") final int page
    ) {
        List<InfoSummaryResponse> responses = noteInfoQueryService.findInfoSummariesByCategory(category, page);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{category}")
    public ResponseEntity<List<InfoSummaryResponse>> findBestNoteInfoSummariesByCategory(
            @PathVariable("category") final Category category
    ) {
        List<InfoSummaryResponse> responses = noteInfoQueryService.findBestInfoSummariesByCategory(category);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/author/{authorId}/{category}")
    public ResponseEntity<List<InfoSummaryResponse>> findHisBestNoteInfoSummaries(
            @PathVariable("authorId") final Long authorId,
            @PathVariable("category") final Category category
    ) {
        List<InfoSummaryResponse> responses = noteInfoQueryService
                .findHisBestInfoSummariesByCategory(authorId, category);
        return ResponseEntity.ok(responses);
    }

    /**
     * findNoteInfoSummaries() 호출 후 필수적으로 호출해주어야 하는 ImageUrl 얻는 API
     * findNoteInfoSummariesByCategory() 호출 후 필수적으로 호출해주어야 하는 ImageUrl 얻는 API
     * findBestNoteInfoSummariesByCategory() 호출 후 필수적으로 호출해주어야 하는 ImageUrl 얻는 API
     * findHisBestNoteInfoSummaries() 호출 후 필수적으로 호출해주어야 하는 ImageUrl 얻는 API
     * @param request Image Keys
     * @return PresignedGetUrls
     */
    @PostMapping("/image/summary")
    public ResponseEntity<List<InfoSummaryUrlResponse>> convertSummariesKeyToUrl(
            @RequestBody final List<InfoSummaryKeyRequest> request
    ) {
        List<InfoSummaryUrlResponse> responses = new ArrayList<>();
        for (InfoSummaryKeyRequest keyRequest : request) {
            InfoSummaryUrlResponse response = new InfoSummaryUrlResponse(
                    keyRequest.getThumbnailKey(),
                    keyRequest.getAuthorProfileKey()
            );
            responses.add(response);
        }
        return ResponseEntity.ok(responses);
    }


}
