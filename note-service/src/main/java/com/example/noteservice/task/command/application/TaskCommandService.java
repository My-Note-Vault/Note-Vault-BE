package com.example.noteservice.task.command.application;

import com.example.noteservice.task.command.domain.Status;
import com.example.noteservice.task.command.domain.Task;
import com.example.noteservice.task.command.domain.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class TaskCommandService {

    private final TaskRepository taskRepository;

    @Transactional
    public Long createTask(
            final Long memberId,
            final String title,
            final String content,
            final Status status
    ) {
        Task task = new Task(memberId, title, content, status);
        taskRepository.save(task);

        return task.getId();
    }

    @Transactional
    public void editTask(
            final Long memberId,
            final Long taskId,
            final String title,
            final String content,
            final Status status
    ) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task 를 찾을 수 없습니다"));

        task.edit(memberId, title, content, status);
    }

    @Transactional
    public void updateStatus(final Long memberId, final Long taskId, final Status status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task 를 찾을 수 없습니다"));

        task.updateStatus(memberId, status);
    }

    @Transactional
    public void deleteTask(final Long memberId, final Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task 를 찾을 수 없습니다"));

        if (!task.getAuthorId().equals(memberId)) {
            throw new IllegalArgumentException("삭제할 권한이 없습니다.");
        }
        taskRepository.delete(task);
    }
}
