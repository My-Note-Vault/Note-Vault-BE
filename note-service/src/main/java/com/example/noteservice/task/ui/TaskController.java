package com.example.noteservice.task.ui;

import com.example.noteservice.task.command.application.TaskCommandService;
import com.example.noteservice.task.command.domain.Task;
import com.example.noteservice.task.query.TaskQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/v1/task")
@RestController
public class TaskController {

    private final TaskCommandService taskCommandService;
    private final TaskQueryService taskQueryService;

    @GetMapping
    public ResponseEntity<Task> findSpecificTask(
            @RequestParam final Long authorId,
            @RequestParam final Long taskId
    ) {
        Task task = taskQueryService.findTaskById(authorId, taskId);
        return ResponseEntity.ok(task);
    }

    @GetMapping
    public ResponseEntity<List<Task>> findAllTasks(@RequestParam final Long authorId) {
        List<Task> allTasks = taskQueryService.findAllTasksByAuthorId(authorId);
        return ResponseEntity.ok(allTasks);
    }


}
