package com.example.platformservice.noteinfo.command.application;

import com.example.common.api.CreateSnapshotRequest;
import com.example.common.api.SnapshotClient;
import com.example.platformservice.noteinfo.command.application.request.CreateNoteInfoRequest;
import com.example.platformservice.noteinfo.command.application.request.UpdateNoteInfoWithoutImageRequest;
import com.example.platformservice.noteinfo.command.domain.NoteInfo;
import com.example.platformservice.noteinfo.command.domain.NoteInfoImage;
import com.example.platformservice.noteinfo.command.domain.NoteInfoRepository;
import com.example.platformservice.noteinfo.command.domain.value.Category;
import com.example.platformservice.noteinfo.command.domain.value.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NoteInfoCommandServiceTest {

    @InjectMocks
    private NoteInfoCommandService noteInfoCommandService;

    @Mock
    private NoteInfoRepository noteInfoRepository;

    @Mock
    private SnapshotClient snapshotClient;

    @Nested
    @DisplayName("createNoteInfo 메소드는")
    class CreateNoteInfoTest {

        private final Long memberId = 1L;
        private final String previewImageId = "preview-image-key";
        private final String blurredImageId = "blurred-image-key";

        @Test
        @DisplayName("NoteInfo를 성공적으로 생성한다")
        void createNoteInfo_success() {
            // given
            CreateNoteInfoRequest request = new CreateNoteInfoRequest(
                    100L,
                    "테스트 제목",
                    "테스트 설명",
                    List.of(Category.RESUME),
                    Status.PUBLIC,
                    BigDecimal.ZERO
            );

            given(snapshotClient.createSnapshot(any(CreateSnapshotRequest.class))).willReturn(1L);
            given(noteInfoRepository.save(any(NoteInfo.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            noteInfoCommandService.createNoteInfo(request, memberId, previewImageId, blurredImageId);

            // then
            ArgumentCaptor<NoteInfo> noteInfoCaptor = ArgumentCaptor.forClass(NoteInfo.class);
            verify(noteInfoRepository).save(noteInfoCaptor.capture());

            NoteInfo savedNoteInfo = noteInfoCaptor.getValue();
            assertThat(savedNoteInfo.getTitle()).isEqualTo("테스트 제목");
            assertThat(savedNoteInfo.getDescription()).isEqualTo("테스트 설명");
            assertThat(savedNoteInfo.getAuthorId()).isEqualTo(memberId);
            assertThat(savedNoteInfo.getStatus()).isEqualTo(Status.PUBLIC);
        }

        @Test
        @DisplayName("FOR_SALE 상태일 때 가격이 설정된다")
        void createNoteInfo_forSale_withPrice() {
            // given
            CreateNoteInfoRequest request = new CreateNoteInfoRequest(
                    100L,
                    "판매용 노트",
                    "판매 설명",
                    List.of(Category.COVER_LETTER),
                    Status.FOR_SALE,
                    new BigDecimal("10000")
            );

            given(snapshotClient.createSnapshot(any(CreateSnapshotRequest.class))).willReturn(1L);
            given(noteInfoRepository.save(any(NoteInfo.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            noteInfoCommandService.createNoteInfo(request, memberId, previewImageId, blurredImageId);

            // then
            ArgumentCaptor<NoteInfo> noteInfoCaptor = ArgumentCaptor.forClass(NoteInfo.class);
            verify(noteInfoRepository).save(noteInfoCaptor.capture());

            NoteInfo savedNoteInfo = noteInfoCaptor.getValue();
            assertThat(savedNoteInfo.getStatus()).isEqualTo(Status.FOR_SALE);
            assertThat(savedNoteInfo.getPrice()).isEqualByComparingTo(new BigDecimal("10000"));
        }
    }

    @Nested
    @DisplayName("uploadImage 메소드는")
    class UploadImageTest {

        private final Long noteInfoId = 1L;
        private final String imageKey = "test-image-key";

        @Test
        @DisplayName("이미지를 성공적으로 업로드한다")
        void uploadImage_success() throws Exception {
            // given
            NoteInfo noteInfo = createNoteInfo(noteInfoId);
            given(noteInfoRepository.findById(noteInfoId)).willReturn(Optional.of(noteInfo));

            // when
            noteInfoCommandService.uploadImage(noteInfoId, imageKey);

            // then
            assertThat(noteInfo.getImages()).hasSize(1);
        }

        @Test
        @DisplayName("존재하지 않는 NoteInfo면 예외가 발생한다")
        void uploadImage_notFound() {
            // given
            given(noteInfoRepository.findById(noteInfoId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> noteInfoCommandService.uploadImage(noteInfoId, imageKey))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("유효한 NoteInfo 가 없습니다");
        }
    }

    @Nested
    @DisplayName("deleteNoteInfo 메소드는")
    class DeleteNoteInfoTest {

        private final Long noteInfoId = 1L;
        private final Long memberId = 1L;
        private final Long snapshotId = 100L;

        @Test
        @DisplayName("작성자가 NoteInfo를 성공적으로 삭제한다")
        void deleteNoteInfo_success() throws Exception {
            // given
            NoteInfo noteInfo = createNoteInfoWithSnapshot(noteInfoId, memberId, snapshotId);
            given(noteInfoRepository.findById(noteInfoId)).willReturn(Optional.of(noteInfo));

            // when
            noteInfoCommandService.deleteNoteInfo(noteInfoId, memberId);

            // then
            verify(snapshotClient).deleteSnapshot(snapshotId);
            verify(noteInfoRepository).delete(noteInfo);
        }

        @Test
        @DisplayName("존재하지 않는 NoteInfo면 예외가 발생한다")
        void deleteNoteInfo_notFound() {
            // given
            given(noteInfoRepository.findById(noteInfoId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> noteInfoCommandService.deleteNoteInfo(noteInfoId, memberId))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("해당하는 NoteInfo 가 없습니다!");
        }

        @Test
        @DisplayName("작성자가 아니면 예외가 발생한다")
        void deleteNoteInfo_notAuthor() throws Exception {
            // given
            Long anotherMemberId = 2L;
            NoteInfo noteInfo = createNoteInfoWithSnapshot(noteInfoId, anotherMemberId, snapshotId);
            given(noteInfoRepository.findById(noteInfoId)).willReturn(Optional.of(noteInfo));

            // when & then
            assertThatThrownBy(() -> noteInfoCommandService.deleteNoteInfo(noteInfoId, memberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("작성자만 삭제할 수 있습니다!");
        }
    }

    @Nested
    @DisplayName("deleteImage 메소드는")
    class DeleteImageTest {

        private final Long noteInfoId = 1L;
        private final Long memberId = 1L;

        @Test
        @DisplayName("이미지를 성공적으로 삭제한다")
        void deleteImage_success() throws Exception {
            // given
            NoteInfo noteInfo = createNoteInfoWithImages(noteInfoId, memberId);
            given(noteInfoRepository.findByIdAndAuthorId(noteInfoId, memberId)).willReturn(Optional.of(noteInfo));

            int initialSize = noteInfo.getImages().size();

            // when
            noteInfoCommandService.deleteImage(noteInfoId, memberId, 0);

            // then
            assertThat(noteInfo.getImages()).hasSize(initialSize - 1);
        }

        @Test
        @DisplayName("유효하지 않은 NoteInfo면 예외가 발생한다")
        void deleteImage_notFound() {
            // given
            given(noteInfoRepository.findByIdAndAuthorId(noteInfoId, memberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> noteInfoCommandService.deleteImage(noteInfoId, memberId, 0))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("유효한 NoteInfo 가 없습니다");
        }
    }

    @Nested
    @DisplayName("updateWithoutImage 메소드는")
    class UpdateWithoutImageTest {

        private final Long noteInfoId = 1L;
        private final Long memberId = 1L;

        @Test
        @DisplayName("이미지를 제외한 정보를 성공적으로 업데이트한다")
        void updateWithoutImage_success() throws Exception {
            // given
            NoteInfo noteInfo = createNoteInfo(noteInfoId, memberId);
            UpdateNoteInfoWithoutImageRequest request = new UpdateNoteInfoWithoutImageRequest(
                    noteInfoId,
                    100L,
                    "수정된 제목",
                    "수정된 설명",
                    List.of("image-key"),
                    List.of(Category.OTHER),
                    Status.PRIVATE,
                    BigDecimal.ZERO
            );

            given(noteInfoRepository.findByIdAndAuthorId(noteInfoId, memberId)).willReturn(Optional.of(noteInfo));

            // when
            noteInfoCommandService.updateWithoutImage(request, memberId);

            // then
            assertThat(noteInfo.getTitle()).isEqualTo("수정된 제목");
            assertThat(noteInfo.getDescription()).isEqualTo("수정된 설명");
            assertThat(noteInfo.getStatus()).isEqualTo(Status.PRIVATE);
        }

        @Test
        @DisplayName("유효하지 않은 NoteInfo면 예외가 발생한다")
        void updateWithoutImage_notFound() {
            // given
            UpdateNoteInfoWithoutImageRequest request = new UpdateNoteInfoWithoutImageRequest(
                    noteInfoId,
                    100L,
                    "수정된 제목",
                    "수정된 설명",
                    List.of("image-key"),
                    List.of(Category.OTHER),
                    Status.PRIVATE,
                    BigDecimal.ZERO
            );

            given(noteInfoRepository.findByIdAndAuthorId(noteInfoId, memberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> noteInfoCommandService.updateWithoutImage(request, memberId))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("유효한 NoteInfo 가 없습니다");
        }
    }

    private NoteInfo createNoteInfo(Long id) throws Exception {
        return createNoteInfo(id, 1L);
    }

    private NoteInfo createNoteInfo(Long id, Long authorId) throws Exception {
        NoteInfo noteInfo = NoteInfo.builder()
                .snapshotId(100L)
                .authorId(authorId)
                .title("테스트 제목")
                .description("테스트 설명")
                .price(BigDecimal.ZERO)
                .status(Status.PUBLIC)
                .categories(new ArrayList<>())
                .build();

        setId(noteInfo, id);
        return noteInfo;
    }

    private NoteInfo createNoteInfoWithSnapshot(Long id, Long authorId, Long snapshotId) throws Exception {
        NoteInfo noteInfo = NoteInfo.builder()
                .snapshotId(snapshotId)
                .authorId(authorId)
                .title("테스트 제목")
                .description("테스트 설명")
                .price(BigDecimal.ZERO)
                .status(Status.PUBLIC)
                .categories(new ArrayList<>())
                .build();

        setId(noteInfo, id);
        return noteInfo;
    }

    private NoteInfo createNoteInfoWithImages(Long id, Long authorId) throws Exception {
        NoteInfo noteInfo = createNoteInfo(id, authorId);
        noteInfo.uploadImages(new NoteInfoImage("image-key-1"));
        noteInfo.uploadImages(new NoteInfoImage("image-key-2"));
        return noteInfo;
    }

    private void setId(NoteInfo noteInfo, Long id) throws Exception {
        Field idField = NoteInfo.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(noteInfo, id);
    }
}
