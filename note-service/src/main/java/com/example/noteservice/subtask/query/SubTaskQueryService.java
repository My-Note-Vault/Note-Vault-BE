package com.example.noteservice.subtask.query;

import com.example.noteservice.subtask.command.domain.SubTask;
import com.example.noteservice.subtask.command.domain.SubTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class SubTaskQueryService {

    private final SubTaskRepository subTaskRepository;

    public SubTask findTaskById(
            final Long authorId,
            final Long subTaskId
    ) {
        SubTask subTask = subTaskRepository.findById(subTaskId)
                .orElseThrow(() -> new NoSuchElementException("Task 를 찾을 수 없습니다"));

        if (!subTask.getAuthorId().equals(authorId)) {
            throw new IllegalArgumentException("조회가 허용되지 않았습니다");
        }
        return subTask;
    }

    public List<SubTask> findAllTasksByAuthorId(final Long authorId) {
        return subTaskRepository.findAllByAuthorId(authorId);
    }


}
