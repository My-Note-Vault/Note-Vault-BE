package com.example.workspace.task.command.application;

import com.example.common.file.image.ImageUtils;
import com.example.workspace.task.command.domain.Task;
import com.example.workspace.task.command.domain.TaskRepository;
import com.example.workspace.task.command.domain.value.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.example.common.CommonConstant.CANNOT_FIND_TASK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TaskCommandServiceTest {

    @InjectMocks
    private TaskCommandService taskCommandService;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ImageUtils imageUtils;

    @Nested
    @DisplayName("createTask 메소드는")
    class CreateTaskTest {

        @Test
        @DisplayName("기본 Task를 저장한다")
        void createTask_success() {
            taskCommandService.createTask(1L, 2L);

            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
            verify(taskRepository).save(taskCaptor.capture());

            Task savedTask = taskCaptor.getValue();
            assertThat(savedTask.getWorkSpaceId()).isEqualTo(1L);
            assertThat(savedTask.getAuthorId()).isEqualTo(2L);
            assertThat(savedTask.getTitle()).isEqualTo("새 Task");
            assertThat(savedTask.getContent()).isEmpty();
            assertThat(savedTask.getIsPublic()).isFalse();
            assertThat(savedTask.getSchedule().getStatus()).isEqualTo(Status.NOT_STARTED);
        }
    }

    @Nested
    @DisplayName("editTask 메소드는")
    class EditTaskTest {

        @Test
        @DisplayName("작성자가 Task를 수정하면 변경 내용을 저장하고 제거된 이미지를 정리한다")
        void editTask_success() {
            Task task = new Task(1L, 2L);
            LocalDateTime startDateTime = LocalDateTime.of(2026, 1, 1, 10, 0);
            LocalDateTime endDateTime = LocalDateTime.of(2026, 1, 1, 11, 0);
            given(taskRepository.findById(3L)).willReturn(Optional.of(task));

            taskCommandService.editTask(
                    2L,
                    3L,
                    "수정된 Task",
                    "new-content",
                    Status.IN_PROGRESS,
                    startDateTime,
                    endDateTime,
                    true
            );

            assertThat(task.getTitle()).isEqualTo("수정된 Task");
            assertThat(task.getContent()).isEqualTo("new-content");
            assertThat(task.getSchedule().getStatus()).isEqualTo(Status.IN_PROGRESS);
            assertThat(task.getSchedule().getStartDateTime()).isEqualTo(startDateTime);
            assertThat(task.getSchedule().getEndDateTime()).isEqualTo(endDateTime);
            assertThat(task.getIsPublic()).isTrue();
            verify(imageUtils).deleteRemovedContentImages("", "new-content");
            verify(taskRepository).save(task);
        }

        @Test
        @DisplayName("존재하지 않는 Task면 예외가 발생한다")
        void editTask_taskNotFound() {
            given(taskRepository.findById(3L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> taskCommandService.editTask(
                    2L,
                    3L,
                    "수정된 Task",
                    "new-content",
                    Status.IN_PROGRESS,
                    null,
                    null,
                    true
            ))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage(CANNOT_FIND_TASK);

            verify(imageUtils, never()).deleteRemovedContentImages(null, "new-content");
        }
    }

    @Nested
    @DisplayName("deleteTask 메소드는")
    class DeleteTaskTest {

        @Test
        @DisplayName("작성자가 Task를 삭제하면 본문 이미지를 모두 정리하고 삭제한다")
        void deleteTask_success() {
            Task task = new Task(1L, 2L, Status.NOT_STARTED, null, null, "Task", "content", false);
            given(taskRepository.findById(3L)).willReturn(Optional.of(task));

            taskCommandService.deleteTask(2L, 3L);

            verify(imageUtils).deleteAllContentImages("content");
            verify(taskRepository).delete(task);
        }

        @Test
        @DisplayName("작성자가 아니면 삭제할 수 없다")
        void deleteTask_forbidden() {
            Task task = new Task(1L, 2L);
            given(taskRepository.findById(3L)).willReturn(Optional.of(task));

            assertThatThrownBy(() -> taskCommandService.deleteTask(9L, 3L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("삭제할 권한이 없습니다.");

            verify(imageUtils, never()).deleteAllContentImages(task.getContent());
            verify(taskRepository, never()).delete(task);
        }
    }
}
