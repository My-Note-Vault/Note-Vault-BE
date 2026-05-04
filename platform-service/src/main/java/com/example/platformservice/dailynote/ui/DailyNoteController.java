package com.example.platformservice.dailynote.ui;

import com.example.common.AuthMemberId;
import com.example.platformservice.dailynote.application.DailyNoteService;
import com.example.platformservice.dailynote.application.request.AddPlanRequest;
import com.example.platformservice.dailynote.application.request.DeletePlanRequest;
import com.example.platformservice.dailynote.application.request.EditDailyNoteRequest;
import com.example.platformservice.dailynote.application.request.EditPlanRequest;
import com.example.platformservice.dailynote.application.response.DailyNoteDetailResponse;
import com.example.platformservice.dailynote.application.response.DailyNoteSimpleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static com.example.platformservice.PlatformConst.DAILY_NOTES_BASIC_PATH;


@RequiredArgsConstructor
@RequestMapping(DAILY_NOTES_BASIC_PATH)
@RestController
public class DailyNoteController {

    private final DailyNoteService dailyNoteService;

    @GetMapping({"/date/{date}"})
    public ResponseEntity<DailyNoteDetailResponse> findSpecificDailyNote(
            @PathVariable("date") final LocalDate date,
            @AuthMemberId final Long authorId
    ) {
        DailyNoteDetailResponse dailyNoteDetailResponse = dailyNoteService.findDailyNoteByDate(authorId, date);
        return ResponseEntity.ok(dailyNoteDetailResponse);
    }

    @GetMapping({"/{dailyNoteId}"})
    public ResponseEntity<DailyNoteDetailResponse> findSpecificDailyNote(
            @PathVariable("dailyNoteId") final Long dailyNoteId,
            @AuthMemberId final Long authorId
    ) {
        DailyNoteDetailResponse dailyNoteDetailResponse = dailyNoteService.findDailyNoteById(authorId, dailyNoteId);
        return ResponseEntity.ok(dailyNoteDetailResponse);
    }

    @GetMapping
    public ResponseEntity<DailyNoteDetailResponse> findTodayDailyNote(@AuthMemberId final Long authorId) {
        DailyNoteDetailResponse todayDailyNoteDetailResponse = dailyNoteService.getTodayDailyNote(authorId);
        return ResponseEntity.ok(todayDailyNoteDetailResponse);
    }

    @GetMapping("/all")
    public ResponseEntity<List<DailyNoteSimpleResponse>> findAllDailyNotes(@AuthMemberId final Long authorId) {
        List<DailyNoteSimpleResponse> dailyNoteSimpleResponses = dailyNoteService.findAllDailyNotesByAuthorId(authorId);
        return ResponseEntity.ok(dailyNoteSimpleResponses);
    }

    @PatchMapping("/{dailyNoteId}")
    public ResponseEntity<Void> editDailyNote(
            @PathVariable final Long dailyNoteId,
            @RequestBody final EditDailyNoteRequest request,
            @AuthMemberId final Long memberId
    ) {
        dailyNoteService.editDailyNote(
                memberId,
                dailyNoteId,
                request.getContent()
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{dailyNoteId}/plans")
    public ResponseEntity<Long> addPlan(
            @PathVariable final Long dailyNoteId,
            @RequestBody final AddPlanRequest request,
            @AuthMemberId final Long memberId
    ) {
        Long planId = dailyNoteService.addPlan(memberId, dailyNoteId, request.getType(), request.getContent());
        return ResponseEntity.ok(planId);
    }

    @PatchMapping("/{dailyNoteId}/plans")
    public ResponseEntity<Void> editPlan(
            @PathVariable final Long dailyNoteId,
            @RequestBody final EditPlanRequest request,
            @AuthMemberId final Long memberId
    ) {
        dailyNoteService.editPlan(
                memberId,
                dailyNoteId,
                request.getPlanId(),
                request.getType(),
                request.getContent(),
                request.getIsDone()
        );
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{dailyNoteId}/plans")
    public ResponseEntity<Void> deletePlan(
            @PathVariable final Long dailyNoteId,
            @RequestBody final DeletePlanRequest request,
            @AuthMemberId final Long memberId
    ) {
        dailyNoteService.deletePlan(memberId, dailyNoteId, request.getPlanId());
        return ResponseEntity.noContent().build();
    }


    @DeleteMapping("/{dailyNoteId}")
    public ResponseEntity<Void> deleteDailyNote(
            @PathVariable final Long dailyNoteId,
            @AuthMemberId final Long memberId
    ) {
        dailyNoteService.deleteDailyNote(memberId, dailyNoteId);
        return ResponseEntity.noContent().build();
    }
}
