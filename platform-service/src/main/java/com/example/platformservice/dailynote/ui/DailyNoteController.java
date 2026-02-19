package com.example.platformservice.dailynote.ui;

import com.example.platformservice.dailynote.command.application.DailyNoteCommandService;
import com.example.platformservice.dailynote.command.application.request.CreateDailyNoteRequest;
import com.example.platformservice.dailynote.command.application.request.EditDailyNoteRequest;
import com.example.platformservice.dailynote.command.domain.DailyNote;
import com.example.platformservice.dailynote.query.DailyNoteQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.common.CommonUtils.AUTHORIZED_MEMBER_ID;

@RequiredArgsConstructor
@RequestMapping("/api/v1/daily-note")
@RestController
public class DailyNoteController {

    private final DailyNoteCommandService dailyNoteCommandService;
    private final DailyNoteQueryService triviaQueryService;

    @GetMapping
    public ResponseEntity<DailyNote> findSpecificDailyNote(
            @RequestParam final Long authorId,
            @RequestParam final Long dailyNoteId
    ) {
        DailyNote dailyNote = triviaQueryService.findDailyNoteById(authorId, dailyNoteId);
        return ResponseEntity.ok(dailyNote);
    }

    @GetMapping("/all")
    public ResponseEntity<List<DailyNote>> findAllDailyNotes(@RequestParam final Long authorId) {
        List<DailyNote> allDailyNotes = triviaQueryService.findAllDailyNotesByAuthorId(authorId);
        return ResponseEntity.ok(allDailyNotes);
    }

    @PostMapping
    public ResponseEntity<Long> createDailyNote(
            @RequestBody final CreateDailyNoteRequest request,
            @RequestAttribute(AUTHORIZED_MEMBER_ID) final Long memberId
    ) {
        Long dailyNoteId = dailyNoteCommandService.createDailyNote(
                memberId,
                request.getTodayTodoList(),
                request.getTomorrowTodoList(),
                request.getMemo(),
                request.getIsCollapsed()
        );
        return ResponseEntity.ok(dailyNoteId);
    }

    @PatchMapping
    public ResponseEntity<Void> editDailyNote(
            @RequestBody final EditDailyNoteRequest request,
            @RequestAttribute(AUTHORIZED_MEMBER_ID) final Long memberId
    ) {
        dailyNoteCommandService.editDailyNote(
                memberId,
                request.getDailyNoteId(),
                request.getTodayTodoList(),
                request.getTomorrowTodoList(),
                request.getMemo(),
                request.getIsCollapsed()
        );
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{dailyNoteId}")
    public ResponseEntity<Void> deleteDailyNote(
            @PathVariable final Long dailyNoteId,
            @RequestAttribute(AUTHORIZED_MEMBER_ID) final Long memberId
    ) {
        dailyNoteCommandService.deleteDailyNote(memberId, dailyNoteId);
        return ResponseEntity.noContent().build();
    }


}
