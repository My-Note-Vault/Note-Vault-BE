package com.example.noteservice.task.ui;

import com.example.noteservice.task.command.application.TaskCommandService;
import com.example.noteservice.task.command.application.request.CreateTaskRequest;
import com.example.noteservice.task.command.application.request.EditTaskRequest;
import com.example.noteservice.task.command.domain.Task;
import com.example.noteservice.task.query.TaskQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.common.CommonUtils.AUTHORIZED_MEMBER_ID;

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

    @PostMapping
    public ResponseEntity<Long> createTask(
            @RequestBody final CreateTaskRequest request,
            @RequestAttribute(AUTHORIZED_MEMBER_ID) final Long memberId
    ) {
        Long taskId = taskCommandService.createTask(
                memberId,
                request.getTitle(),
                request.getContent(),
                request.getStatus()
        );
        return ResponseEntity.ok(taskId);
    }

    @PatchMapping
    public ResponseEntity<Void> editTask(
            @RequestBody final EditTaskRequest request,
            @RequestAttribute(AUTHORIZED_MEMBER_ID) final Long memberId
    ) {
        taskCommandService.editTask(
                memberId,
                request.getTaskId(),
                request.getTitle(),
                request.getContent(),
                request.getStatus()
        );
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable final Long taskId,
            @RequestAttribute(AUTHORIZED_MEMBER_ID) final Long memberId
    ) {
        taskCommandService.deleteTask(memberId, taskId);
        return ResponseEntity.noContent().build();
    }


}
