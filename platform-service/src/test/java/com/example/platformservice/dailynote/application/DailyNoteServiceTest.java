package com.example.platformservice.dailynote.application;

import com.example.common.exception.ConflictException;
import com.example.common.file.image.ImageUtils;
import com.example.platformservice.dailynote.domain.*;
import com.example.platformservice.member.domain.value.DayStartTime;
import com.example.platformservice.member.infra.MemberRepository;
import com.example.search.sync.SearchSyncRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyNoteServiceTest {

    @Mock DailyNoteRepository dailyNoteRepository;
    @Mock DailyNoteFolderRepository dailyNoteFolderRepository;
    @Mock PlanRepository planRepository;
    @Mock DailyNotePlanRepository dailyNotePlanRepository;
    @Mock MemberRepository memberRepository;
    @Mock ImageUtils imageUtils;
    @Mock SearchSyncRecorder searchSyncRecorder;

    private DailyNoteService dailyNoteService;

    @BeforeEach
    void setUp() {
        dailyNoteService = new DailyNoteService(dailyNoteRepository, dailyNoteFolderRepository,
                planRepository, dailyNotePlanRepository, memberRepository, imageUtils, searchSyncRecorder);
    }

    @Test
    @DisplayName("일일 노트 revision이 일치하면 수정 후 다음 revision의 검색 이벤트를 기록한다")
    void contentRevision이_일치하면_내용을_수정하고_다음_revision을_반환한다() {
        Long authorId = 1L;
        Long dailyNoteId = 2L;
        DailyNote dailyNote = new DailyNote(authorId, "old", new DayStartTime(0, 0));
        when(dailyNoteRepository.findByIdAndAuthorId(dailyNoteId, authorId)).thenReturn(Optional.of(dailyNote));
        when(dailyNoteRepository.updateContentIfRevisionMatches(dailyNoteId, authorId, "new", 3L)).thenReturn(1);

        long revision = dailyNoteService.editDailyNote(authorId, dailyNoteId, "new", 3L);

        assertThat(revision).isEqualTo(4L);
        verify(imageUtils).deleteRemovedContentImages("old", "new");
        verify(searchSyncRecorder).refreshDailyNote(dailyNoteId, 4L);
    }

    @Test
    @DisplayName("일일 노트 revision 충돌 시 이미지 정리와 검색 이벤트 기록을 수행하지 않는다")
    void contentRevision이_다르면_충돌로_처리하고_이미지를_삭제하지_않는다() {
        Long authorId = 1L;
        Long dailyNoteId = 2L;
        DailyNote dailyNote = new DailyNote(authorId, "latest", new DayStartTime(0, 0));
        when(dailyNoteRepository.findByIdAndAuthorId(dailyNoteId, authorId)).thenReturn(Optional.of(dailyNote));
        when(dailyNoteRepository.updateContentIfRevisionMatches(dailyNoteId, authorId, "stale", 2L)).thenReturn(0);

        assertThatThrownBy(() -> dailyNoteService.editDailyNote(authorId, dailyNoteId, "stale", 2L))
                .isInstanceOf(ConflictException.class);
        verify(imageUtils, never()).deleteRemovedContentImages("latest", "stale");
        verifyNoInteractions(searchSyncRecorder);
    }

    @Test
    @DisplayName("음수 revision은 저장소와 외부 연동을 호출하기 전에 거부한다")
    void rejectsNegativeRevision() {
        assertThatThrownBy(() -> dailyNoteService.editDailyNote(1L, 2L, "new", -1L))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(dailyNoteRepository, imageUtils, searchSyncRecorder);
    }

    @Test
    @DisplayName("일일 노트 작성자가 아니면 삭제와 검색 삭제 이벤트 기록을 거부한다")
    void rejectsDeletionByAnotherMember() {
        var note = new DailyNote(1L, "content", new DayStartTime(0, 0));
        when(dailyNoteRepository.findWithWriteLockById(2L)).thenReturn(Optional.of(note));
        assertThatThrownBy(() -> dailyNoteService.deleteDailyNote(9L, 2L))
                .isInstanceOf(IllegalArgumentException.class);
        verify(dailyNoteRepository, never()).delete(any());
        verifyNoInteractions(imageUtils, searchSyncRecorder, dailyNotePlanRepository);
    }

    @Test
    @DisplayName("일일 노트를 삭제하면 검색 삭제 이벤트와 연결된 계획 정리도 수행한다")
    void recordsDeleteEvent() {
        var note = new DailyNote(1L, "content", new DayStartTime(0, 0));
        when(dailyNoteRepository.findWithWriteLockById(2L)).thenReturn(Optional.of(note));

        dailyNoteService.deleteDailyNote(1L, 2L);

        verify(searchSyncRecorder).deleteDailyNote(2L);
        verify(dailyNotePlanRepository).deleteAllByDailyNote(note);
        verify(dailyNoteRepository).delete(note);
        verify(imageUtils).deleteAllContentImages("content");
    }
}
