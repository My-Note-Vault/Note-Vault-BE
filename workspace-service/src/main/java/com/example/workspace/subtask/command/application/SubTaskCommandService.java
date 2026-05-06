package com.example.workspace.subtask.command.application;

import com.example.workspace.subtask.command.domain.SubTask;
import com.example.workspace.subtask.command.domain.SubTaskRepository;
import com.example.workspace.task.command.domain.value.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class SubTaskCommandService {

    private final SubTaskRepository subTaskRepository;

    @Transactional
    public Long createSubTask(
            final Long memberId,
            final Long taskId,
            final LocalDateTime startDateTime,
            final LocalDateTime endDateTime,
            final Status status,
            final String title,
            final String content
    ) {
        SubTask subTask = new SubTask(
                memberId,
                taskId,
                startDateTime,
                endDateTime,
                status,
                title,
                content
        );
        subTaskRepository.save(subTask);

        return subTask.getId();
    }


    @Transactional
    public void editSubTask(
            final Long memberId,
            final Long subTaskId,
            final String title,
            final String content,
            final Status status,
            final LocalDateTime startDateTime,
            final LocalDateTime endDateTime
    ) {
        SubTask subTask = subTaskRepository.findById(subTaskId)
                .orElseThrow(() -> new NoSuchElementException("Task 를 찾을 수 없습니다"));

        subTask.edit(memberId, title, content, status, startDateTime, endDateTime);
        subTaskRepository.save(subTask);
    }

    @Transactional
    public void deleteSubTask(final Long memberId, final Long subTaskId) {
        SubTask subTask = subTaskRepository.findById(subTaskId)
                .orElseThrow(() -> new NoSuchElementException("Task 를 찾을 수 없습니다"));

        if (!subTask.getAuthorId().equals(memberId)) {
            throw new IllegalArgumentException("삭제할 권한이 없습니다.");
        }
        subTaskRepository.delete(subTask);
    }
}
