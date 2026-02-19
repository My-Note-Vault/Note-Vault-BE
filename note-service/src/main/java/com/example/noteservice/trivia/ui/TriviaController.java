package com.example.noteservice.trivia.ui;

import com.example.noteservice.trivia.command.application.TriviaCommandService;
import com.example.noteservice.trivia.command.application.request.CreateTriviaRequest;
import com.example.noteservice.trivia.command.application.request.EditTriviaRequest;
import com.example.noteservice.trivia.command.domain.Trivia;
import com.example.noteservice.trivia.query.TriviaQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.common.CommonUtils.AUTHORIZED_MEMBER_ID;

@RequiredArgsConstructor
@RequestMapping("/api/v1/trivia")
@RestController
public class TriviaController {

    private final TriviaCommandService triviaCommandService;
    private final TriviaQueryService triviaQueryService;

    @GetMapping
    public ResponseEntity<Trivia> findSpecificTrivia(
            @RequestParam final Long authorId,
            @RequestParam final Long triviaId
    ) {
        Trivia task = triviaQueryService.findTriviaById(authorId, triviaId);
        return ResponseEntity.ok(task);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Trivia>> findAllTrivia(@RequestParam final Long authorId) {
        List<Trivia> allTasks = triviaQueryService.findAllTriviaByAuthorId(authorId);
        return ResponseEntity.ok(allTasks);
    }

    @PostMapping
    public ResponseEntity<Long> createTrivia(
            @RequestBody final CreateTriviaRequest request,
            @RequestAttribute(AUTHORIZED_MEMBER_ID) final Long memberId
    ) {
        Long subTaskId = triviaCommandService.createTrivia(
                memberId,
                request.getParentTaskId(),
                request.getTitle(),
                request.getContent(),
                request.getIsHidden()
        );
        return ResponseEntity.ok(subTaskId);
    }

    @PatchMapping
    public ResponseEntity<Void> editTrivia(
            @RequestBody final EditTriviaRequest request,
            @RequestAttribute(AUTHORIZED_MEMBER_ID) final Long memberId
    ) {
        triviaCommandService.editTrivia(
                memberId,
                request.getParentTaskId(),
                request.getTitle(),
                request.getContent(),
                request.getIsHidden()
        );
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{triviaId}")
    public ResponseEntity<Void> deleteTrivia(
            @PathVariable final Long triviaId,
            @RequestAttribute(AUTHORIZED_MEMBER_ID) final Long memberId
    ) {
        triviaCommandService.deleteTrivia(memberId, triviaId);
        return ResponseEntity.noContent().build();
    }


}
