package com.example.platformservice.viewed.application;

import com.example.platformservice.viewed.domain.RecentlyViewedId;
import com.example.platformservice.viewed.domain.RecentlyViewedInfo;
import com.example.platformservice.viewed.domain.RecentlyViewedInfoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecentlyViewedInfoServiceTest {

    @InjectMocks
    private RecentlyViewedInfoService recentlyViewedInfoService;

    @Mock
    private RecentlyViewedInfoRepository recentlyViewedInfoRepository;

    @Nested
    @DisplayName("save 메소드는")
    class SaveTest {

        private final Long noteInfoId = 100L;
        private final Long memberId = 1L;

        @Test
        @DisplayName("최근 본 상품을 성공적으로 저장한다")
        void save_success() {
            // given
            given(recentlyViewedInfoRepository.save(any(RecentlyViewedInfo.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            RecentlyViewedId result = recentlyViewedInfoService.save(noteInfoId, memberId);

            // then
            assertThat(result.getMemberId()).isEqualTo(memberId);
            assertThat(result.getNoteInfoId()).isEqualTo(noteInfoId);

            ArgumentCaptor<RecentlyViewedInfo> captor = ArgumentCaptor.forClass(RecentlyViewedInfo.class);
            verify(recentlyViewedInfoRepository).save(captor.capture());

            RecentlyViewedInfo savedInfo = captor.getValue();
            assertThat(savedInfo.getId().getMemberId()).isEqualTo(memberId);
            assertThat(savedInfo.getId().getNoteInfoId()).isEqualTo(noteInfoId);
        }

        @Test
        @DisplayName("이미 존재하는 경우 viewedAt을 업데이트한다")
        void save_update_whenDuplicate() {
            // given
            RecentlyViewedId id = new RecentlyViewedId(memberId, noteInfoId);
            RecentlyViewedInfo existingInfo = new RecentlyViewedInfo(id);

            given(recentlyViewedInfoRepository.save(any(RecentlyViewedInfo.class)))
                    .willThrow(new DataIntegrityViolationException("Duplicate"));
            given(recentlyViewedInfoRepository.findById(id)).willReturn(Optional.of(existingInfo));

            // when
            RecentlyViewedId result = recentlyViewedInfoService.save(noteInfoId, memberId);

            // then
            assertThat(result.getMemberId()).isEqualTo(memberId);
            assertThat(result.getNoteInfoId()).isEqualTo(noteInfoId);
        }

        @Test
        @DisplayName("중복인데 조회도 안 되면 예외가 발생한다")
        void save_duplicateButNotFound() {
            // given
            RecentlyViewedId id = new RecentlyViewedId(memberId, noteInfoId);

            given(recentlyViewedInfoRepository.save(any(RecentlyViewedInfo.class)))
                    .willThrow(new DataIntegrityViolationException("Duplicate"));
            given(recentlyViewedInfoRepository.findById(id)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> recentlyViewedInfoService.save(noteInfoId, memberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("최근 본 상품의 정합성이 깨졌습니다.");
        }
    }

    @Nested
    @DisplayName("delete 메소드는")
    class DeleteTest {

        private final Long noteInfoId = 100L;
        private final Long memberId = 1L;

        @Test
        @DisplayName("최근 본 상품을 성공적으로 삭제한다")
        void delete_success() {
            // when
            recentlyViewedInfoService.delete(noteInfoId, memberId);

            // then
            ArgumentCaptor<RecentlyViewedId> captor = ArgumentCaptor.forClass(RecentlyViewedId.class);
            verify(recentlyViewedInfoRepository).deleteById(captor.capture());

            RecentlyViewedId deletedId = captor.getValue();
            assertThat(deletedId.getMemberId()).isEqualTo(memberId);
            assertThat(deletedId.getNoteInfoId()).isEqualTo(noteInfoId);
        }
    }
}
