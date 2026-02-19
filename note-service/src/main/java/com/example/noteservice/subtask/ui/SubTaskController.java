package com.example.noteservice.subtask.ui;

import com.example.noteservice.subtask.command.application.SubTaskCommandService;
import com.example.noteservice.subtask.command.application.request.CreateSubTaskRequest;
import com.example.noteservice.subtask.command.application.request.EditSubTaskRequest;
import com.example.noteservice.subtask.command.domain.SubTask;
import com.example.noteservice.subtask.query.SubTaskQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.common.CommonUtils.AUTHORIZED_MEMBER_ID;

@RequiredArgsConstructor
@RequestMapping("/api/v1/subTask")
@RestController
public class SubTaskController {

    private final SubTaskCommandService subTaskCommandService;
    private final SubTaskQueryService taskQueryService;

    @GetMapping
    public ResponseEntity<SubTask> findSpecificSubTask(
            @RequestParam final Long authorId,
            @RequestParam final Long taskId
    ) {
        SubTask task = taskQueryService.findSubTaskById(authorId, taskId);
        return ResponseEntity.ok(task);
    }

    @GetMapping("/all")
    public ResponseEntity<List<SubTask>> findAllSubTasks(@RequestParam final Long authorId) {
        List<SubTask> allTasks = taskQueryService.findAllSubTasksByAuthorId(authorId);
        return ResponseEntity.ok(allTasks);
    }

    @PostMapping
    public ResponseEntity<Long> createSubTask(
            @RequestBody final CreateSubTaskRequest request,
            @RequestAttribute(AUTHORIZED_MEMBER_ID) final Long memberId
    ) {
        Long subTaskId = subTaskCommandService.createSubTask(
                memberId,
                request.getTaskId(),
                request.getTitle(),
                request.getContent(),
                request.getStatus()
        );
        return ResponseEntity.ok(subTaskId);
    }

    @PatchMapping
    public ResponseEntity<Void> editSubTask(
            @RequestBody final EditSubTaskRequest request,
            @RequestAttribute(AUTHORIZED_MEMBER_ID) final Long memberId
    ) {
        subTaskCommandService.editSubTask(
                memberId,
                request.getSubTaskId(),
                request.getTitle(),
                request.getContent(),
                request.getStatus()
        );
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{subTaskId}")
    public ResponseEntity<Void> deleteSubTask(
            @PathVariable final Long subTaskId,
            @RequestAttribute(AUTHORIZED_MEMBER_ID) final Long memberId
    ) {
        subTaskCommandService.deleteSubTask(memberId, subTaskId);
        return ResponseEntity.noContent().build();
    }


}
