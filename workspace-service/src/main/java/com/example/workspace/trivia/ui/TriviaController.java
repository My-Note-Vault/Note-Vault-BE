package com.example.workspace.trivia.ui;

import com.example.common.AuthMemberId;
import com.example.workspace.trivia.command.application.TriviaCommandService;
import com.example.workspace.trivia.command.application.request.CreateTriviaRequest;
import com.example.workspace.trivia.command.application.request.EditTriviaRequest;
import com.example.workspace.trivia.command.domain.Trivia;
import com.example.workspace.trivia.query.TriviaQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RequiredArgsConstructor
@RequestMapping("/api/v1/trivia")
@RestController
public class TriviaController {

    private final TriviaCommandService triviaCommandService;
    private final TriviaQueryService triviaQueryService;

    @GetMapping("/{id}")
    public ResponseEntity<Trivia> findSpecificTrivia(
            @PathVariable("id") final Long triviaId,
            @AuthMemberId final Long authorId
    ) {
        Trivia task = triviaQueryService.findTriviaById(authorId, triviaId);
        return ResponseEntity.ok(task);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Trivia>> findAllTrivia(@AuthMemberId final Long authorId) {
        List<Trivia> allTasks = triviaQueryService.findAllTriviaByAuthorId(authorId);
        return ResponseEntity.ok(allTasks);
    }

    @PostMapping
    public ResponseEntity<Long> createTrivia(
            @RequestBody final CreateTriviaRequest request,
            @AuthMemberId final Long memberId
    ) {
        Long subTaskId = triviaCommandService.createTrivia(memberId, request.getSubTaskId());
        return ResponseEntity.ok(subTaskId);
    }

    @PatchMapping
    public ResponseEntity<Void> editTrivia(
            @RequestBody final EditTriviaRequest request,
            @AuthMemberId final Long memberId
    ) {
        triviaCommandService.editTrivia(
                memberId,
                request.getTriviaId(),
                request.getParentTaskId(),
                request.getTitle(),
                request.getContent(),
                request.getIsPublic()
        );
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{triviaId}")
    public ResponseEntity<Void> deleteTrivia(
            @PathVariable final Long triviaId,
            @AuthMemberId final Long memberId
    ) {
        triviaCommandService.deleteTrivia(memberId, triviaId);
        return ResponseEntity.noContent().build();
    }


}
