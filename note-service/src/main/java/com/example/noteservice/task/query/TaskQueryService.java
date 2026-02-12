package com.example.noteservice.task.query;

import com.example.noteservice.task.command.domain.Task;
import com.example.noteservice.task.command.domain.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class TaskQueryService {

    private final TaskRepository taskRepository;

    public Task findTaskById(
            final Long authorId,
            final Long taskId
    ) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task 를 찾을 수 없습니다"));

        if (!task.getAuthorId().equals(authorId)) {
            throw new IllegalArgumentException("조회가 허용되지 않았습니다");
        }
        return task;
    }

    public List<Task> findAllTasksByAuthorId(final Long authorId) {
        return taskRepository.findAllByAuthorId(authorId);
    }


}
