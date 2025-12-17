package com.example.platformservice.noteinfo.query;

import com.example.common.api.NoteReader;
import com.example.common.api.SnapshotDetailResponse;
import com.example.common.file.ImageUtils;
import com.example.platformservice.noteinfo.command.domain.NoteInfo;
import com.example.platformservice.noteinfo.command.domain.NoteInfoRepository;
import com.example.platformservice.noteinfo.command.domain.value.Category;
import com.example.platformservice.noteinfo.command.domain.value.Status;
import com.example.platformservice.noteinfo.query.response.InfoSummaryResponse;
import com.example.platformservice.noteinfo.query.response.NoteInfoDetailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class NoteInfoQueryServiceTest {

    @InjectMocks
    private NoteInfoQueryService noteInfoQueryService;

    @Mock
    private NoteInfoRepository noteInfoRepository;

    @Mock
    private NoteReader noteReader;

    @Mock
    private ImageUtils imageUtils;

    @Nested
    @DisplayName("findNoteInfoById 메소드는")
    class FindNoteInfoByIdTest {

        private final Long noteInfoId = 1L;

        @Test
        @DisplayName("PUBLIC 상태의 NoteInfo를 성공적으로 조회한다")
        void findNoteInfoById_public_success() throws Exception {
            // given
            NoteInfo noteInfo = createNoteInfo(noteInfoId, 1L, Status.PUBLIC, 100L);
            SnapshotDetailResponse snapshotDetail = SnapshotDetailResponse.builder()
                    .snapshotId(100L)
                    .authorId(1L)
                    .title("제목")
                    .content("내용")
                    .previewImageKey("preview-key")
                    .blurredImageKey("blurred-key")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            given(noteInfoRepository.findByIdAndStatusNot(noteInfoId, Status.PRIVATE))
                    .willReturn(Optional.of(noteInfo));
            given(noteReader.getSnapshotDetail(100L)).willReturn(snapshotDetail);

            // when
            NoteInfoDetailResponse result = noteInfoQueryService.findNoteInfoById(noteInfoId);

            // then
            assertThat(result.getInfoId()).isEqualTo(noteInfoId);
            assertThat(result.getSnapshotImageKey()).isEqualTo("preview-key");
        }

        @Test
        @DisplayName("FOR_SALE 상태의 NoteInfo는 블러 이미지를 반환한다")
        void findNoteInfoById_forSale_returnsBlurredImage() throws Exception {
            // given
            NoteInfo noteInfo = createNoteInfo(noteInfoId, 1L, Status.FOR_SALE, 100L);
            SnapshotDetailResponse snapshotDetail = SnapshotDetailResponse.builder()
                    .snapshotId(100L)
                    .authorId(1L)
                    .title("제목")
                    .content("내용")
                    .previewImageKey("preview-key")
                    .blurredImageKey("blurred-key")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            given(noteInfoRepository.findByIdAndStatusNot(noteInfoId, Status.PRIVATE))
                    .willReturn(Optional.of(noteInfo));
            given(noteReader.getSnapshotDetail(100L)).willReturn(snapshotDetail);

            // when
            NoteInfoDetailResponse result = noteInfoQueryService.findNoteInfoById(noteInfoId);

            // then
            assertThat(result.getSnapshotImageKey()).isEqualTo("blurred-key");
        }

        @Test
        @DisplayName("존재하지 않거나 PRIVATE 상태면 예외가 발생한다")
        void findNoteInfoById_notFound() {
            // given
            given(noteInfoRepository.findByIdAndStatusNot(noteInfoId, Status.PRIVATE))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> noteInfoQueryService.findNoteInfoById(noteInfoId))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("No such NoteInfo");
        }
    }

    @Nested
    @DisplayName("findInfoSummaries 메소드는")
    class FindInfoSummariesTest {

        @Test
        @DisplayName("페이징된 전체 목록을 반환한다")
        void findInfoSummaries_success() {
            // given
            List<InfoSummaryResponse> summaries = List.of(
                    new InfoSummaryResponse("thumb1", "제목1", 10, 4.5, 100, "profile1", "작성자1"),
                    new InfoSummaryResponse("thumb2", "제목2", 5, 3.5, 50, "profile2", "작성자2")
            );

            given(noteInfoRepository.findPagedInfoSummaries(0)).willReturn(summaries);

            // when
            List<InfoSummaryResponse> result = noteInfoQueryService.findInfoSummaries(0);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getTitle()).isEqualTo("제목1");
        }

        @Test
        @DisplayName("결과가 없으면 빈 리스트를 반환한다")
        void findInfoSummaries_empty() {
            // given
            given(noteInfoRepository.findPagedInfoSummaries(0)).willReturn(Collections.emptyList());

            // when
            List<InfoSummaryResponse> result = noteInfoQueryService.findInfoSummaries(0);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findInfoSummariesByCategory 메소드는")
    class FindInfoSummariesByCategoryTest {

        @Test
        @DisplayName("카테고리별 목록을 반환한다")
        void findInfoSummariesByCategory_success() {
            // given
            List<InfoSummaryResponse> summaries = List.of(
                    new InfoSummaryResponse("thumb1", "이력서1", 10, 4.5, 100, "profile1", "작성자1")
            );

            given(noteInfoRepository.findPagedInfoSummariesByCategory(Category.RESUME, 0))
                    .willReturn(summaries);

            // when
            List<InfoSummaryResponse> result = noteInfoQueryService.findInfoSummariesByCategory(Category.RESUME, 0);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("이력서1");
        }
    }

    @Nested
    @DisplayName("findBestInfoSummariesByCategory 메소드는")
    class FindBestInfoSummariesByCategoryTest {

        @Test
        @DisplayName("카테고리별 상위 5개를 반환한다")
        void findBestInfoSummariesByCategory_success() {
            // given
            List<InfoSummaryResponse> summaries = List.of(
                    new InfoSummaryResponse("thumb1", "베스트1", 100, 5.0, 500, "profile1", "작성자1"),
                    new InfoSummaryResponse("thumb2", "베스트2", 90, 4.8, 400, "profile2", "작성자2")
            );

            given(noteInfoRepository.findBestInfoSummariesByCategory(Category.COVER_LETTER))
                    .willReturn(summaries);

            // when
            List<InfoSummaryResponse> result = noteInfoQueryService.findBestInfoSummariesByCategory(Category.COVER_LETTER);

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("findHisBestInfoSummariesByCategory 메소드는")
    class FindHisBestInfoSummariesByCategoryTest {

        @Test
        @DisplayName("특정 작성자의 카테고리별 상위 5개를 반환한다")
        void findHisBestInfoSummariesByCategory_success() {
            // given
            Long authorId = 1L;
            List<InfoSummaryResponse> summaries = List.of(
                    new InfoSummaryResponse("thumb1", "내 베스트1", 50, 4.5, 200, "profile1", "작성자1")
            );

            given(noteInfoRepository.findHisBestInfoSummariesByCategory(authorId, Category.OTHER))
                    .willReturn(summaries);

            // when
            List<InfoSummaryResponse> result = noteInfoQueryService.findHisBestInfoSummariesByCategory(authorId, Category.OTHER);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAuthorName()).isEqualTo("작성자1");
        }
    }

    private NoteInfo createNoteInfo(Long id, Long authorId, Status status, Long snapshotId) throws Exception {
        NoteInfo noteInfo = NoteInfo.builder()
                .snapshotId(snapshotId)
                .authorId(authorId)
                .title("테스트 제목")
                .description("테스트 설명")
                .price(BigDecimal.ZERO)
                .status(status)
                .categories(new ArrayList<>())
                .build();

        Field idField = NoteInfo.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(noteInfo, id);

        return noteInfo;
    }
}
