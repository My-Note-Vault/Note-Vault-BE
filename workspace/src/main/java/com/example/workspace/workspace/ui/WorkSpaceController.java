package com.example.workspace.workspace.ui;

import com.example.workspace.workspace.command.application.WorkSpaceCommandService;
import com.example.workspace.workspace.command.application.request.CreateWorkSpaceRequest;
import com.example.workspace.workspace.command.application.request.EditWorkSpaceRequest;
import com.example.workspace.workspace.command.application.request.UpdateParticipantsRequest;
import com.example.workspace.workspace.command.domain.WorkSpace;
import com.example.workspace.workspace.query.WorkSpaceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.common.CommonUtils.AUTHORIZED_MEMBER_ID;

@RequiredArgsConstructor
@RequestMapping("/api/v1/workspace")
@RestController
public class WorkSpaceController {

    private final WorkSpaceCommandService workSpaceCommandService;
    private final WorkSpaceQueryService workSpaceQueryService;

    @GetMapping
    public ResponseEntity<WorkSpace> findSpecificWorkSpace(
            @RequestParam final Long workSpaceId,
            @RequestAttribute(AUTHORIZED_MEMBER_ID) final Long memberId
    ) {
        WorkSpace workSpace = workSpaceQueryService.findWorkSpaceById(memberId, workSpaceId);
        return ResponseEntity.ok(workSpace);
    }

    @GetMapping("/all")
    public ResponseEntity<List<WorkSpace>> findAllWorkSpaces(
            @RequestAttribute(AUTHORIZED_MEMBER_ID) final Long memberId
    ) {
        List<WorkSpace> allWorkSpaces = workSpaceQueryService.findAllWorkSpacesByCreatorId(memberId);
        return ResponseEntity.ok(allWorkSpaces);
    }

    @PostMapping
    public ResponseEntity<Long> createWorkSpace(
            @RequestBody final CreateWorkSpaceRequest request,
            @RequestAttribute(AUTHORIZED_MEMBER_ID) final Long memberId
    ) {
        Long workSpaceId = workSpaceCommandService.createWorkSpace(
                memberId,
                request.getParentId(),
                request.getName(),
                request.getContent(),
                request.getIsPublic()
        );
        return ResponseEntity.ok(workSpaceId);
    }

    @PutMapping("/participants")
    public ResponseEntity<Void> updateParticipants(
            @RequestBody final UpdateParticipantsRequest request,
            @RequestAttribute(AUTHORIZED_MEMBER_ID) final Long memberId
    ) {
        workSpaceCommandService.updateParticipants(
                memberId, request.getWorkSpaceId(),
                request.getMemberIdsToAdd(),
                request.getMemberIdsToRemove()
        );
        return ResponseEntity.noContent().build();
    }

    @PatchMapping
    public ResponseEntity<Void> editWorkSpace(
            @RequestBody final EditWorkSpaceRequest request,
            @RequestAttribute(AUTHORIZED_MEMBER_ID) final Long memberId
    ) {
        workSpaceCommandService.editWorkSpace(
                memberId,
                request.getWorkSpaceId(),
                request.getParentId(),
                request.getName(),
                request.getContent(),
                request.getIsPublic()
        );
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/leave/{workSpaceId}")
    public ResponseEntity<Void> leaveWorkSpace(
            @PathVariable final Long workSpaceId,
            @RequestAttribute(AUTHORIZED_MEMBER_ID) final Long memberId
    ) {
        workSpaceCommandService.leaveWorkSpace(memberId, workSpaceId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{workSpaceId}")
    public ResponseEntity<Void> deleteWorkSpace(
            @PathVariable final Long workSpaceId,
            @RequestAttribute(AUTHORIZED_MEMBER_ID) final Long memberId
    ) {
        workSpaceCommandService.deleteWorkSpace(memberId, workSpaceId);
        return ResponseEntity.noContent().build();
    }


}
