package com.example.workspace.subtask.command.application;

import com.example.workspace.subtask.command.domain.Status;
import com.example.workspace.subtask.command.domain.SubTask;
import com.example.workspace.subtask.command.domain.SubTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class SubTaskCommandService {

    private final SubTaskRepository subTaskRepository;

    @Transactional
    public Long createSubTask(
            final Long memberId,
            final Long taskId,
            final String title,
            final String content,
            final Status status
    ) {
        SubTask subTask = new SubTask(memberId, taskId, title, content, status);
        subTaskRepository.save(subTask);

        return subTask.getId();
    }


    @Transactional
    public void editSubTask(final Long memberId, final Long subTaskId, final String title, final String content, final Status status) {
        SubTask subTask = subTaskRepository.findById(subTaskId)
                .orElseThrow(() -> new NoSuchElementException("Task 를 찾을 수 없습니다"));

        subTask.edit(memberId, title, content, status);
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
