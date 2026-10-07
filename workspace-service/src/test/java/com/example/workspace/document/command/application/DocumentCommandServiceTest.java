package com.example.workspace.document.command.application;

import com.example.common.file.image.ImageUtils;
import com.example.common.exception.ForbiddenException;
import com.example.search.sync.SearchSyncRecorder;
import com.example.workspace.document.command.domain.Document;
import com.example.workspace.document.command.domain.DocumentDelta;
import com.example.workspace.document.command.domain.DocumentDeltaRepository;
import com.example.workspace.document.command.domain.DocumentEditActivityRepository;
import com.example.workspace.document.command.domain.DocumentRepository;
import com.example.workspace.document.command.domain.DocumentType;
import com.example.workspace.workspace.command.domain.Participant;
import com.example.workspace.workspace.command.domain.ParticipantRepository;
import com.example.workspace.workspace.command.domain.WorkSpace;
import com.example.workspace.workspace.command.domain.WorkSpaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentCommandServiceTest {

    @InjectMocks
    private DocumentCommandService documentCommandService;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private ImageUtils imageUtils;

    @Mock private DocumentDeltaRepository documentDeltaRepository;
    @Mock private DocumentEditActivityRepository documentEditActivityRepository;
    @Mock private WorkSpaceRepository workSpaceRepository;
    @Mock private SearchSyncRecorder searchSyncRecorder;
    @Mock private DocumentRefreshRequests refreshRequests;

    @Test
    @DisplayName("createDocument 메소드는 TASK 또는 NOTE 부모 아래 NOTE 문서를 저장한다")
    void createNote_success() {
        Document parentNote = Document.note(10L, null, 1L);
        given(documentRepository.findById(2L))
                .willReturn(Optional.of(parentNote));
        given(participantRepository.findByWorkSpaceIdAndMemberId(10L, 1L))
                .willReturn(Optional.of(new Participant(10L, 1L)));
        given(workSpaceRepository.findWithWriteLockById(10L))
                .willReturn(Optional.of(new WorkSpace(1L, "워크스페이스", "", false)));
        given(documentRepository.save(any(Document.class))).willAnswer(invocation -> {
            Document saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 3L);
            return saved;
        });

        documentCommandService.createDocument(1L, DocumentType.NOTE, null, 2L);

        verify(documentRepository).save(argThat(document ->
                document.getType() == DocumentType.NOTE
                        && document.getAuthorId().equals(1L)
                        && document.getParentId().equals(2L)
                        && document.getSearchContent().isEmpty()
        ));
        verify(searchSyncRecorder).refreshDocument(3L, 0L);
    }

    @Test
    @DisplayName("editDocument 메소드는 NOTE 문서의 제목과 공개 여부를 수정한다")
    void editNote_success() {
        Document note = Document.note(10L, null, 1L);
        ReflectionTestUtils.setField(note, "id", 3L);
        given(documentRepository.findWithLockByIdAndType(3L, DocumentType.NOTE))
                .willReturn(Optional.of(note));
        given(participantRepository.findByWorkSpaceIdAndMemberId(10L, 1L))
                .willReturn(Optional.of(new Participant(10L, 1L)));

        documentCommandService.editDocument(
                1L,
                3L,
                DocumentType.NOTE,
                null,
                "수정된 Document",
                null,
                null,
                null,
                true
        );

        assertThat(note.getTitle()).isEqualTo("수정된 Document");
        assertThat(note.getIsPublic()).isTrue();
        verify(documentRepository).save(note);
        verify(searchSyncRecorder).refreshDocument(3L, 0L);
    }

    @Test
    @DisplayName("deleteDocument 메소드는 NOTE 문서 본문 이미지를 정리하고 삭제한다")
    void deleteNote_success() {
        Document note = Document.note(10L, null, 1L);
        given(documentRepository.findWithLockByIdAndType(3L, DocumentType.NOTE))
                .willReturn(Optional.of(note));
        given(participantRepository.findByWorkSpaceIdAndMemberId(10L, 1L))
                .willReturn(Optional.of(new Participant(10L, 1L)));
        given(documentRepository.findAllByParentId(3L)).willReturn(List.of());

        documentCommandService.deleteDocument(1L, 3L, DocumentType.NOTE);

        verify(imageUtils).deleteAllContentImages("");
        verify(documentRepository).delete(note);
        verify(searchSyncRecorder).deleteDocument(3L);
    }

    @Test
    @DisplayName("새 CRDT update를 저장하면 revision을 증가시키고 검색 갱신 배치를 기록한다")
    void appendsDeltaAndRecordsRefresh() {
        Document note = lockedNote();
        given(documentDeltaRepository.save(any(DocumentDelta.class))).willAnswer(invocation -> invocation.getArgument(0));

        var committed = documentCommandService.appendDocumentDelta(1L, 10L, 3L, DocumentType.NOTE,
                "update-1", 5, new byte[]{1, 2});

        assertThat(committed.revision()).isEqualTo(1L);
        assertThat(note.getLatestRevision()).isEqualTo(1L);
        verify(documentDeltaRepository).save(any(DocumentDelta.class));
        verify(documentEditActivityRepository).save(any());
        verify(refreshRequests).recordDelta(note);
    }

    @Test
    @DisplayName("동일 clientUpdateId의 재전송은 revision과 검색 이벤트를 중복 생성하지 않는다")
    void duplicateDeltaReturnsCommittedRevision() {
        Document note = lockedNote();
        long revision = note.issueNextRevision();
        var existing = new DocumentDelta(note, revision, "update-1", new byte[]{1, 2});
        given(documentDeltaRepository.findByDocumentIdAndClientUpdateId(3L, "update-1"))
                .willReturn(Optional.of(existing));

        var committed = documentCommandService.appendDocumentDelta(1L, 10L, 3L, DocumentType.NOTE,
                "update-1", 5, new byte[]{9});

        assertThat(committed.revision()).isEqualTo(1L);
        assertThat(note.getLatestRevision()).isEqualTo(1L);
        verify(documentDeltaRepository, never()).save(any());
        verify(documentRepository, never()).save(any());
        verifyNoInteractions(refreshRequests, searchSyncRecorder, documentEditActivityRepository);
    }

    @Test
    @DisplayName("다른 워크스페이스의 문서에는 CRDT update를 추가할 수 없다")
    void rejectsDeltaForAnotherWorkspace() {
        lockedNote();
        assertThatThrownBy(() -> documentCommandService.appendDocumentDelta(1L, 99L, 3L,
                DocumentType.NOTE, "update-1", 0, new byte[]{1}))
                .isInstanceOf(java.util.NoSuchElementException.class);
        verifyNoInteractions(documentDeltaRepository, refreshRequests, documentEditActivityRepository);
    }

    @Test
    @DisplayName("제목이 변하지 않으면 공개 여부를 수정해도 검색 이벤트를 기록하지 않는다")
    void unchangedTitleSkipsRefresh() {
        Document note = lockedNote();
        documentCommandService.editDocument(1L, 3L, DocumentType.NOTE, null,
                note.getTitle(), null, null, null, true);
        assertThat(note.getIsPublic()).isTrue();
        verifyNoInteractions(searchSyncRecorder);
    }

    @Test
    @DisplayName("문서 작성자가 아닌 참여자는 삭제와 검색 삭제 이벤트를 수행할 수 없다")
    void rejectsDeletionByAnotherParticipant() {
        Document note = Document.note(10L, null, 9L);
        given(documentRepository.findWithLockByIdAndType(3L, DocumentType.NOTE)).willReturn(Optional.of(note));
        given(participantRepository.findByWorkSpaceIdAndMemberId(10L, 1L))
                .willReturn(Optional.of(new Participant(10L, 1L)));

        assertThatThrownBy(() -> documentCommandService.deleteDocument(1L, 3L, DocumentType.NOTE))
                .isInstanceOf(ForbiddenException.class);
        verify(documentRepository, never()).delete(any());
        verifyNoInteractions(imageUtils, searchSyncRecorder);
    }

    private Document lockedNote() {
        Document note = Document.note(10L, null, 1L);
        ReflectionTestUtils.setField(note, "id", 3L);
        given(documentRepository.findWithLockByIdAndType(3L, DocumentType.NOTE)).willReturn(Optional.of(note));
        given(participantRepository.findByWorkSpaceIdAndMemberId(10L, 1L))
                .willReturn(Optional.of(new Participant(10L, 1L)));
        return note;
    }
}
