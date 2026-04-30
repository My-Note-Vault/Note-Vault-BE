package com.example.workspace.task.command.application;

import com.example.workspace.task.command.domain.Task;
import com.example.workspace.task.command.domain.TaskRepository;
import com.example.workspace.task.command.domain.value.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import static com.example.common.CommonConstant.CANNOT_FIND_TASK;

@RequiredArgsConstructor
@Service
public class TaskCommandService {

    private final TaskRepository taskRepository;

    @Transactional
    public Long createTask(final Long workSpaceId, final Long authorId) {
        Task task = new Task(workSpaceId, authorId);
        taskRepository.save(task);

        return task.getId();
    }

    @Transactional
    public void editTask(
            final Long memberId,
            final Long taskId,
            final String title,
            final String content,
            final Status status,
            final LocalDateTime startDateTime,
            final LocalDateTime endDateTime
    ) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException(CANNOT_FIND_TASK));

        task.edit(memberId, title, content, status, startDateTime, endDateTime);
    }

    @Transactional
    public void updateStatus(final Long memberId, final Long taskId, final Status status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException(CANNOT_FIND_TASK));

        task.updateStatus(memberId, status);
    }

    @Transactional
    public void deleteTask(final Long memberId, final Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException(CANNOT_FIND_TASK));

        if (!task.getAuthorId().equals(memberId)) {
            throw new IllegalArgumentException("삭제할 권한이 없습니다.");
        }
        taskRepository.delete(task);
    }
}
