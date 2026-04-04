package com.example.platformservice.dailynote.ui;

import com.example.common.AuthMemberId;
import com.example.platformservice.dailynote.command.application.DailyNoteCommandService;
import com.example.platformservice.dailynote.command.application.request.CreateDailyNoteRequest;
import com.example.platformservice.dailynote.command.application.request.EditDailyNoteRequest;
import com.example.platformservice.dailynote.query.DailyNoteQueryService;
import com.example.platformservice.dailynote.query.response.DailyNoteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.platformservice.Const.DAILY_NOTES_BASIC_PATH;

@RequiredArgsConstructor
@RequestMapping(DAILY_NOTES_BASIC_PATH)
@RestController
public class DailyNoteController {

    private final DailyNoteCommandService dailyNoteCommandService;
    private final DailyNoteQueryService dailyNoteQueryService;

    @GetMapping({"/{dailyNoteId}"})
    public ResponseEntity<DailyNoteResponse> findSpecificDailyNote(
            @PathVariable("dailyNoteId") final Long dailyNoteId,
            @AuthMemberId final Long authorId
    ) {
        DailyNoteResponse dailyNoteResponse = dailyNoteQueryService.findDailyNoteById(authorId, dailyNoteId);
        return ResponseEntity.ok(dailyNoteResponse);
    }

    @GetMapping
    public ResponseEntity<DailyNoteResponse> findTodayDailyNote(@AuthMemberId final Long authorId) {
        DailyNoteResponse todayDailyNoteResponse = dailyNoteQueryService.getTodayDailyNote(authorId);
        return ResponseEntity.ok(todayDailyNoteResponse);
    }

    @GetMapping("/all")
    public ResponseEntity<List<DailyNoteResponse>> findAllDailyNotes(@AuthMemberId final Long authorId) {
        List<DailyNoteResponse> allDailyNotes = dailyNoteQueryService.findAllDailyNotesByAuthorId(authorId);
        return ResponseEntity.ok(allDailyNotes);
    }

    @PostMapping
    public ResponseEntity<Long> createDailyNote(
            @RequestBody final CreateDailyNoteRequest request,
            @AuthMemberId final Long memberId
    ) {
        Long dailyNoteId = dailyNoteCommandService.createDailyNoteAtFirst(
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
            @AuthMemberId final Long memberId
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
            @AuthMemberId final Long memberId
    ) {
        dailyNoteCommandService.deleteDailyNote(memberId, dailyNoteId);
        return ResponseEntity.noContent().build();
    }
}
