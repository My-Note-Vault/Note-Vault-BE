package com.example.noteservice.task.command.application;

import com.example.noteservice.task.command.application.request.EditTaskRequest;
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
    public Long createTask(final Long memberId) {
        Task task = new Task(memberId);
        taskRepository.save(task);

        return task.getId();
    }


    @Transactional
    public void editTask(final EditTaskRequest request) {
        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new NoSuchElementException("Task 를 찾을 수 없습니다"));

        task.edit(
                request.getAuthorId(),
                request.getTitle(),
                request.getContent()
        );
    }

/*
    @Transactional
    public void deleteNote(final Long noteId, final Long memberId) {
        Task note = taskRepository.findById(noteId)
                .orElseThrow(() -> new NoSuchElementException("해당하는 노트가 없습니다"));

        if (!note.getAuthorId().equals(memberId)) {
            throw new NoSuchElementException("작성자만 삭제할 수 있습니다");
        }
        taskRepository.delete(note);
    }*/

}
