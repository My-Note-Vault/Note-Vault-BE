package com.example.noteservice.note.ui;

import com.example.noteservice.note.command.application.NoteCommandService;
import com.example.noteservice.note.command.application.request.CreateNoteRequest;
import com.example.noteservice.note.command.application.request.EditNoteRequest;
import com.example.noteservice.note.query.response.EditNoteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/api/v1/note")
@RestController
public class NoteController {

    private final NoteCommandService noteService;

    @PostMapping
    public ResponseEntity<Long> createNote(
            @RequestBody final CreateNoteRequest request
    ) {
        Long noteId = noteService.createNote(request.getAuthorId());
        return ResponseEntity.ok(noteId);
    }

    @PutMapping
    public ResponseEntity<EditNoteResponse> editNote(
            @RequestBody final EditNoteRequest request
    ) {
        EditNoteResponse response = noteService.editNote(request);
        return ResponseEntity.ok(response);
    }
}
