package com.example.workspace.workspace.command.application;

import com.example.common.file.image.ImageUtils;
import com.example.workspace.workspace.command.domain.Invitation;
import com.example.workspace.workspace.command.domain.InvitationRepository;
import com.example.workspace.workspace.command.domain.Participant;
import com.example.workspace.workspace.command.domain.ParticipantRepository;
import com.example.workspace.workspace.command.domain.WorkSpace;
import com.example.workspace.workspace.command.domain.WorkSpaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.example.workspace.common.WorkspaceConst.EXPIRED_INVITATION_MESSAGE;
import static com.example.workspace.common.WorkspaceConst.NO_INVITATION_MESSAGE;
import static com.example.workspace.common.WorkspaceConst.NO_PARTICIPANT_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkSpaceCommandServiceTest {

    @InjectMocks
    private WorkSpaceCommandService workSpaceCommandService;

    @Mock
    private WorkSpaceRepository workSpaceRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private ImageUtils imageUtils;

    @Nested
    @DisplayName("createWorkSpace 메소드는")
    class CreateWorkSpaceTest {

        @Test
        @DisplayName("WorkSpace와 생성자 Participant를 저장한다")
        void createWorkSpace_success() {
            workSpaceCommandService.createWorkSpace(1L, "워크스페이스", "content", true);

            ArgumentCaptor<WorkSpace> workSpaceCaptor = ArgumentCaptor.forClass(WorkSpace.class);
            ArgumentCaptor<Participant> participantCaptor = ArgumentCaptor.forClass(Participant.class);
            verify(workSpaceRepository).save(workSpaceCaptor.capture());
            verify(participantRepository).save(participantCaptor.capture());

            WorkSpace savedWorkSpace = workSpaceCaptor.getValue();
            Participant savedParticipant = participantCaptor.getValue();
            assertThat(savedWorkSpace.getCreatorId()).isEqualTo(1L);
            assertThat(savedWorkSpace.getName()).isEqualTo("워크스페이스");
            assertThat(savedWorkSpace.getContent()).isEqualTo("content");
            assertThat(savedWorkSpace.getIsPublic()).isTrue();
            assertThat(savedParticipant.getMemberId()).isEqualTo(1L);
            assertThat(savedParticipant.getWorkSpaceId()).isEqualTo(savedWorkSpace.getId());
        }
    }

    @Nested
    @DisplayName("editWorkSpace 메소드는")
    class EditWorkSpaceTest {

        @Test
        @DisplayName("생성자가 WorkSpace를 수정하면 제거된 이미지를 정리한다")
        void editWorkSpace_success() {
            WorkSpace workSpace = new WorkSpace(1L, "기존", "old-content", false);
            given(workSpaceRepository.findById(2L)).willReturn(Optional.of(workSpace));

            workSpaceCommandService.editWorkSpace(1L, 2L, "수정", "new-content", true);

            assertThat(workSpace.getName()).isEqualTo("수정");
            assertThat(workSpace.getContent()).isEqualTo("new-content");
            assertThat(workSpace.getIsPublic()).isTrue();
            verify(imageUtils).deleteRemovedContentImages("old-content", "new-content");
        }

        @Test
        @DisplayName("생성자가 아니면 수정할 수 없다")
        void editWorkSpace_forbidden() {
            WorkSpace workSpace = new WorkSpace(1L, "기존", "old-content", false);
            given(workSpaceRepository.findById(2L)).willReturn(Optional.of(workSpace));

            assertThatThrownBy(() -> workSpaceCommandService.editWorkSpace(9L, 2L, "수정", "new-content", true))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("자신의 WorkSpace가 아닙니다!");

            verify(imageUtils, never()).deleteRemovedContentImages("old-content", "new-content");
        }
    }

    @Nested
    @DisplayName("updateParticipants 메소드는")
    class UpdateParticipantsTest {

        @Test
        @DisplayName("생성자가 참여자를 추가하고 제거한다")
        void updateParticipants_success() {
            WorkSpace workSpace = new WorkSpace(1L, "워크스페이스", "content", false);
            given(workSpaceRepository.findById(2L)).willReturn(Optional.of(workSpace));

            workSpaceCommandService.updateParticipants(1L, 2L, List.of(3L, 4L), List.of(5L));

            ArgumentCaptor<List<Participant>> addCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<List<Participant>> removeCaptor = ArgumentCaptor.forClass(List.class);
            verify(participantRepository).saveAll(addCaptor.capture());
            verify(participantRepository).deleteAll(removeCaptor.capture());

            assertThat(addCaptor.getValue())
                    .extracting(Participant::getMemberId)
                    .containsExactly(3L, 4L);
            assertThat(addCaptor.getValue())
                    .extracting(Participant::getWorkSpaceId)
                    .containsExactly(2L, 2L);
            assertThat(removeCaptor.getValue())
                    .extracting(Participant::getMemberId)
                    .containsExactly(5L);
        }

        @Test
        @DisplayName("생성자가 아니면 참여자를 변경할 수 없다")
        void updateParticipants_forbidden() {
            WorkSpace workSpace = new WorkSpace(1L, "워크스페이스", "content", false);
            given(workSpaceRepository.findById(2L)).willReturn(Optional.of(workSpace));

            assertThatThrownBy(() -> workSpaceCommandService.updateParticipants(9L, 2L, List.of(3L), List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("권한이 부족합니다");

            verify(participantRepository, never()).saveAll(any());
            verify(participantRepository, never()).deleteAll(any());
        }
    }

    @Nested
    @DisplayName("updateLastVisitedPath 메소드는")
    class UpdateLastVisitedPathTest {

        @Test
        @DisplayName("참여자의 마지막 방문 경로를 변경한다")
        void updateLastVisitedPath_success() {
            Participant participant = new Participant(2L, 1L);
            given(participantRepository.findByWorkSpaceIdAndMemberId(2L, 1L)).willReturn(Optional.of(participant));

            workSpaceCommandService.updateLastVisitedPath(2L, 1L, "/workspaces/2/tasks/3");

            assertThat(participant.getLastVisitedPath()).isEqualTo("/workspaces/2/tasks/3");
        }

        @Test
        @DisplayName("참여자가 아니면 예외가 발생한다")
        void updateLastVisitedPath_participantNotFound() {
            given(participantRepository.findByWorkSpaceIdAndMemberId(2L, 1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> workSpaceCommandService.updateLastVisitedPath(2L, 1L, "/workspaces/2"))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage(NO_PARTICIPANT_MESSAGE);
        }
    }

    @Nested
    @DisplayName("deleteWorkSpace 메소드는")
    class DeleteWorkSpaceTest {

        @Test
        @DisplayName("생성자가 WorkSpace를 삭제하면 본문 이미지를 모두 정리하고 삭제한다")
        void deleteWorkSpace_success() {
            WorkSpace workSpace = new WorkSpace(1L, "워크스페이스", "content", false);
            given(workSpaceRepository.findById(2L)).willReturn(Optional.of(workSpace));

            workSpaceCommandService.deleteWorkSpace(1L, 2L);

            verify(imageUtils).deleteAllContentImages("content");
            verify(workSpaceRepository).delete(workSpace);
        }

        @Test
        @DisplayName("생성자가 아니면 삭제할 수 없다")
        void deleteWorkSpace_forbidden() {
            WorkSpace workSpace = new WorkSpace(1L, "워크스페이스", "content", false);
            given(workSpaceRepository.findById(2L)).willReturn(Optional.of(workSpace));

            assertThatThrownBy(() -> workSpaceCommandService.deleteWorkSpace(9L, 2L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("삭제할 권한이 없습니다.");

            verify(imageUtils, never()).deleteAllContentImages("content");
            verify(workSpaceRepository, never()).delete(workSpace);
        }
    }

    @Nested
    @DisplayName("초대 메소드는")
    class InvitationTest {

        @Test
        @DisplayName("초대 링크를 생성하고 코드를 반환한다")
        void createInvitationLink_success() {
            String code = workSpaceCommandService.createInvitationLink(2L);

            ArgumentCaptor<Invitation> invitationCaptor = ArgumentCaptor.forClass(Invitation.class);
            verify(invitationRepository).save(invitationCaptor.capture());

            Invitation invitation = invitationCaptor.getValue();
            assertThat(invitation.getWorkSpaceId()).isEqualTo(2L);
            assertThat(code).isEqualTo(invitation.getCode());
        }

        @Test
        @DisplayName("유효한 초대 코드를 수락하면 참여자를 저장한다")
        void acceptInvitation_success() {
            Invitation invitation = new Invitation(2L, LocalDateTime.now().plusDays(1));
            given(invitationRepository.findByCode("code")).willReturn(Optional.of(invitation));

            workSpaceCommandService.acceptInvitation(1L, "code");

            ArgumentCaptor<Participant> participantCaptor = ArgumentCaptor.forClass(Participant.class);
            verify(participantRepository).save(participantCaptor.capture());
            assertThat(participantCaptor.getValue().getWorkSpaceId()).isEqualTo(2L);
            assertThat(participantCaptor.getValue().getMemberId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("초대 코드가 없으면 예외가 발생한다")
        void acceptInvitation_invitationNotFound() {
            given(invitationRepository.findByCode("code")).willReturn(Optional.empty());

            assertThatThrownBy(() -> workSpaceCommandService.acceptInvitation(1L, "code"))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage(NO_INVITATION_MESSAGE);

            verify(participantRepository, never()).save(any());
        }

        @Test
        @DisplayName("만료된 초대 코드면 예외가 발생한다")
        void acceptInvitation_expired() {
            Invitation invitation = new Invitation(2L, LocalDateTime.now().minusDays(1));
            given(invitationRepository.findByCode("code")).willReturn(Optional.of(invitation));

            assertThatThrownBy(() -> workSpaceCommandService.acceptInvitation(1L, "code"))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage(EXPIRED_INVITATION_MESSAGE);

            verify(participantRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 참여 중이면 전용 예외가 발생한다")
        void acceptInvitation_alreadyInWorkspace() {
            Invitation invitation = new Invitation(2L, LocalDateTime.now().plusDays(1));
            given(invitationRepository.findByCode("code")).willReturn(Optional.of(invitation));
            given(participantRepository.save(any(Participant.class)))
                    .willThrow(new DataIntegrityViolationException("duplicate"));

            assertThatThrownBy(() -> workSpaceCommandService.acceptInvitation(1L, "code"))
                    .isInstanceOf(AlreadyInWorkSpaceException.class)
                    .hasMessage("duplicate");
        }
    }
}
