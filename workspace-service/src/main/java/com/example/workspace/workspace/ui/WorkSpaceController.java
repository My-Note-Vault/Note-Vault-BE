package com.example.workspace.workspace.ui;

import com.example.common.AuthMemberId;
import com.example.workspace.workspace.command.application.WorkSpaceCommandService;
import com.example.workspace.workspace.command.application.request.CreateWorkSpaceRequest;
import com.example.workspace.workspace.command.application.request.EditWorkSpaceRequest;
import com.example.workspace.workspace.command.application.request.UpdateLastVisitedPathRequest;
import com.example.workspace.workspace.command.application.request.UpdateParticipantsRequest;
import com.example.workspace.workspace.command.domain.WorkSpace;
import com.example.workspace.workspace.query.WorkSpaceQueryService;
import com.example.workspace.workspace.query.response.InvitedWorkSpaceSummaryResponse;
import com.example.workspace.workspace.query.response.WorkSpaceSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces")
@RestController
public class WorkSpaceController {

    private final WorkSpaceCommandService workSpaceCommandService;
    private final WorkSpaceQueryService workSpaceQueryService;

    @GetMapping("information/{id}")
    public ResponseEntity<WorkSpace> findSpecificWorkSpace(
            @PathVariable("id") final Long workSpaceId,
            @AuthMemberId final Long memberId
    ) {
        WorkSpace workSpace = workSpaceQueryService.findWorkSpaceById(memberId, workSpaceId);
        return ResponseEntity.ok(workSpace);
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> findSpecificWorkSpaceLastPath(
            @PathVariable("id") final Long workSpaceId,
            @AuthMemberId final Long memberId
    ) {
        String workSpaceLastPath = workSpaceQueryService.findWorkSpaceLastPath(workSpaceId, memberId);
        return ResponseEntity.ok(workSpaceLastPath);
    }

    @GetMapping("/all")
    public ResponseEntity<List<WorkSpaceSummaryResponse>> findAllWorkSpaces(@AuthMemberId final Long memberId) {
        var allParticipatingWorkSpaces = workSpaceQueryService.findAllWorkSpacesByCreatorId(memberId);
        return ResponseEntity.ok(allParticipatingWorkSpaces);
    }

    @PostMapping
    public ResponseEntity<Long> createWorkSpace(
            @RequestBody final CreateWorkSpaceRequest request,
            @AuthMemberId final Long memberId
    ) {
        Long workSpaceId = workSpaceCommandService.createWorkSpace(
                memberId,
                request.getName(),
                request.getContent(),
                request.getIsPublic()
        );
        return ResponseEntity.ok(workSpaceId);
    }

    @PutMapping("/participants")
    public ResponseEntity<Void> updateParticipants(
            @RequestBody final UpdateParticipantsRequest request,
            @AuthMemberId final Long memberId
    ) {
        workSpaceCommandService.updateParticipants(
                memberId, request.getWorkSpaceId(),
                request.getMemberIdsToAdd(),
                request.getMemberIdsToRemove()
        );
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/last-visited-path")
    public ResponseEntity<Void> updateLastVisitedPath(
            @PathVariable("id") final Long workSpaceId,
            @RequestBody final UpdateLastVisitedPathRequest request,
            @AuthMemberId final Long memberId
    ) {
        workSpaceCommandService.updateLastVisitedPath(workSpaceId, memberId, request.getLastVisitedPath());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping
    public ResponseEntity<Void> editWorkSpace(
            @RequestBody final EditWorkSpaceRequest request,
            @AuthMemberId final Long memberId
    ) {
        workSpaceCommandService.editWorkSpace(
                memberId,
                request.getWorkSpaceId(),
                request.getName(),
                request.getContent(),
                request.getIsPublic()
        );
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/leave/{workSpaceId}")
    public ResponseEntity<Void> leaveWorkSpace(
            @PathVariable final Long workSpaceId,
            @AuthMemberId final Long memberId
    ) {
        workSpaceCommandService.leaveWorkSpace(memberId, workSpaceId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{workSpaceId}")
    public ResponseEntity<Void> deleteWorkSpace(
            @PathVariable final Long workSpaceId,
            @AuthMemberId final Long memberId
    ) {
        workSpaceCommandService.deleteWorkSpace(memberId, workSpaceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/invitations")
    public ResponseEntity<InvitedWorkSpaceSummaryResponse> findInvitedWorkSpaceSummary(
            @RequestParam("code") final String code
    ) {
        InvitedWorkSpaceSummaryResponse response = workSpaceQueryService.findInvitedWorkSpaceSummary(code);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{workSpaceId}/invitations")
    public ResponseEntity<String> createInvitationCode(@PathVariable final Long workSpaceId) {
        String code = workSpaceCommandService.createInvitationLink(workSpaceId);
        return ResponseEntity.ok(code);
    }

    @PostMapping("/invitations/accept")
    public ResponseEntity<Void> acceptInvitation(
            @RequestBody final String code,
            @AuthMemberId final Long memberId
    ) {
        workSpaceCommandService.acceptInvitation(memberId, code);
        return ResponseEntity.noContent().build();
    }


}
