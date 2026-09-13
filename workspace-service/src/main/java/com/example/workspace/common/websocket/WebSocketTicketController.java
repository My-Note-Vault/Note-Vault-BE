package com.example.workspace.common.websocket;

import com.example.common.AuthMemberId;
import com.example.workspace.workspace.query.WorkSpaceQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/websocket-tickets")
public class WebSocketTicketController {

    private static final long TICKET_EXPIRATION_SECONDS = 30;

    private final WebSocketTicketStore ticketStore;
    private final WorkSpaceQueryService workSpaceQueryService;

    @PostMapping
    public ResponseEntity<WebSocketTicketResponse> issue(
            @Valid @RequestBody final WebSocketTicketRequest request,
            @AuthMemberId final Long memberId
    ) {
        workSpaceQueryService.findWorkSpaceById(memberId, request.workspaceId());
        String ticket = ticketStore.issue(new WebSocketTicket(
                memberId,
                request.workspaceId(),
                request.documentType(),
                request.documentId()
        ));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new WebSocketTicketResponse(ticket, TICKET_EXPIRATION_SECONDS));
    }

    public record WebSocketTicketRequest(
            @NotNull Long workspaceId,
            @NotBlank @Pattern(regexp = "[A-Za-z_]+") String documentType,
            @NotNull Long documentId
    ) {
    }

    public record WebSocketTicketResponse(String ticket, long expiresInSeconds) {
    }
}
