package com.example.workspace.note.ui;

import com.example.common.AuthMemberId;
import com.example.workspace.note.command.application.NoteCommandService;
import com.example.workspace.note.command.application.request.CreateNoteRequest;
import com.example.workspace.note.command.application.request.EditNoteRequest;
import com.example.workspace.note.command.domain.Note;
import com.example.workspace.note.query.NoteQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RequiredArgsConstructor
@RequestMapping("/api/v1/note")
@RestController
public class NoteController {

    private final NoteCommandService noteCommandService;
    private final NoteQueryService noteQueryService;

    @GetMapping("/{id}")
    public ResponseEntity<Note> findSpecificNote(
            @PathVariable("id") final Long noteId,
            @AuthMemberId final Long authorId
    ) {
        Note task = noteQueryService.findNoteById(authorId, noteId);
        return ResponseEntity.ok(task);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Note>> findAllNote(@AuthMemberId final Long authorId) {
        List<Note> allTasks = noteQueryService.findAllNoteByAuthorId(authorId);
        return ResponseEntity.ok(allTasks);
    }

    @PostMapping
    public ResponseEntity<Long> createNote(
            @RequestBody final CreateNoteRequest request,
            @AuthMemberId final Long memberId
    ) {
        Long subTaskId = noteCommandService.createNote(memberId, request.getSubTaskId());
        return ResponseEntity.ok(subTaskId);
    }

    @PatchMapping
    public ResponseEntity<Void> editNote(
            @RequestBody final EditNoteRequest request,
            @AuthMemberId final Long memberId
    ) {
        noteCommandService.editNote(
                memberId,
                request.getNoteId(),
                request.getParentTaskId(),
                request.getTitle(),
                request.getContent(),
                request.getIsPublic()
        );
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> deleteNote(
            @PathVariable final Long noteId,
            @AuthMemberId final Long memberId
    ) {
        noteCommandService.deleteNote(memberId, noteId);
        return ResponseEntity.noContent().build();
    }


}
