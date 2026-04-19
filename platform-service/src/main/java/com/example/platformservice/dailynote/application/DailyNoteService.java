package com.example.platformservice.dailynote.application;

import com.example.platformservice.dailynote.application.response.DailyNoteDetailResponse;
import com.example.platformservice.dailynote.application.response.DailyNoteSimpleResponse;
import com.example.platformservice.dailynote.application.response.PlanResponse;
import com.example.platformservice.dailynote.domain.*;
import com.example.platformservice.member.domain.Member;
import com.example.platformservice.member.domain.value.DayStartTime;
import com.example.platformservice.member.infra.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.example.platformservice.Const.*;

@RequiredArgsConstructor
@Service
public class DailyNoteService {

    private final DailyNoteRepository dailyNoteRepository;
    private final PlanRepository planRepository;
    private final DailyNotePlanRepository dailyNotePlanRepository;

    private final MemberRepository memberRepository;

    @Transactional
    public Long addPlan(final Long authorId, final Long dailyNoteId, final Type type, final String content) {
        Plan plan = new Plan(type, content);
        planRepository.save(plan);

        DailyNote dailyNote = dailyNoteRepository.findById(dailyNoteId)
                .orElseThrow(() -> new NoSuchElementException(NO_DAILY_NOTE_MESSAGE));

        if (!dailyNote.getAuthorId().equals(authorId)) {
            throw new IllegalArgumentException(ACCESS_DENIED);
        }

        DailyNotePlan dailyNotePlan = new DailyNotePlan(dailyNote, plan);
        dailyNotePlanRepository.save(dailyNotePlan);
        return dailyNotePlan.getId();
    }

    @Transactional
    public void editPlan(
            final Long authorId,
            final Long dailyNoteId,
            final Long planId,
            final Type type,
            final String content,
            final boolean isDone
    ) {
        DailyNote dailyNote = dailyNoteRepository.findById(dailyNoteId)
                .orElseThrow(() -> new NoSuchElementException(NO_DAILY_NOTE_MESSAGE));
        if (!dailyNote.getAuthorId().equals(authorId)) {
            throw new IllegalArgumentException(ACCESS_DENIED);
        }

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new NoSuchElementException(NO_PLAN_MESSAGE));
        plan.edit(type, content, isDone);
    }

    @Transactional
    public void deletePlan(final Long authorId, final Long dailyNoteId, final Long planId) {
        DailyNote dailyNote = dailyNoteRepository.findById(dailyNoteId)
                .orElseThrow(() -> new NoSuchElementException(NO_DAILY_NOTE_MESSAGE));
        if (!dailyNote.getAuthorId().equals(authorId)) {
            throw new IllegalArgumentException(ACCESS_DENIED);
        }

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new NoSuchElementException(NO_PLAN_MESSAGE));

        planRepository.delete(plan);
        dailyNotePlanRepository.deleteAllByPlan(plan);
    }

    @Transactional
    public void editDailyNote(final Long authorId, final Long dailyNoteId, final String content) {
        DailyNote dailyNote = dailyNoteRepository.findByIdAndAuthorId(dailyNoteId, authorId)
                .orElseThrow(() -> new NoSuchElementException(NO_DAILY_NOTE_MESSAGE));

        dailyNote.edit(content);
    }

    @Transactional
    public void deleteDailyNote(final Long memberId, final Long dailyNoteId) {
        DailyNote dailyNote = dailyNoteRepository.findById(dailyNoteId)
                .orElseThrow(() -> new NoSuchElementException(NO_DAILY_NOTE_MESSAGE));

        if (!dailyNote.getAuthorId().equals(memberId)) {
            throw new IllegalArgumentException(ACCESS_DENIED);
        }
        dailyNoteRepository.delete(dailyNote);
        dailyNotePlanRepository.deleteAllByDailyNote(dailyNote);
    }

    @Transactional(readOnly = true)
    public DailyNoteDetailResponse findDailyNoteByDate(final Long authorId, final LocalDate date) {
        DailyNote dailyNote = dailyNoteRepository.findByAuthorIdAndLogicalDate(authorId, date)
                .orElseThrow(() -> new NoSuchElementException(NO_DAILY_NOTE_MESSAGE));

        List<PlanResponse> allIncompletePlans = dailyNotePlanRepository.findAllIncompletePlans(dailyNote.getId()).stream()
                .map(PlanResponse::from)
                .toList();
        return DailyNoteDetailResponse.from(dailyNote, allIncompletePlans);
    }

    @Transactional(readOnly = true)
    public DailyNoteDetailResponse findDailyNoteById(final Long authorId, final Long dailyNoteId) {
        DailyNote dailyNote = dailyNoteRepository.findById(dailyNoteId)
                .orElseThrow(() -> new NoSuchElementException(NO_DAILY_NOTE_MESSAGE));

        if (!dailyNote.getAuthorId().equals(authorId)) {
            throw new IllegalArgumentException(ACCESS_DENIED);
        }

        List<PlanResponse> allIncompletePlans = dailyNotePlanRepository.findAllIncompletePlans(dailyNote.getId()).stream()
                .map(PlanResponse::from)
                .toList();
        return new DailyNoteDetailResponse(dailyNote.getId(), dailyNote.getContent(), dailyNote.getLogicalDate(), allIncompletePlans);
    }

    @Transactional
    public DailyNoteDetailResponse getTodayDailyNote(final Long authorId) {
        Member member = memberRepository.findById(authorId)
                .orElseThrow(() -> new NoSuchElementException(NO_USER_MESSAGE));

        DayStartTime dayStartTime = member.getDayStartTime();
        LocalDate logicalToday = DailyNote.convertToLogicalToday(dayStartTime);
        Optional<DailyNote> todayNote = dailyNoteRepository.findByAuthorIdAndLogicalDate(authorId, logicalToday);

        List<PlanResponse> allIncompletePlanResponses;
        DailyNote dailyNote;

        if (todayNote.isEmpty()) {
            dailyNote = new DailyNote(authorId, dayStartTime);
            dailyNoteRepository.save(dailyNote);

            Optional<DailyNote> latestDailyNote = dailyNoteRepository.findLatestDailyNoteAfter(authorId, logicalToday);
            if (latestDailyNote.isPresent()) {
                Long latestDailyNoteId = latestDailyNote.get().getId();
                List<Plan> allIncompletePlans = dailyNotePlanRepository.findAllIncompletePlans(latestDailyNoteId);

                List<DailyNotePlan> dailyNotePlans = allIncompletePlans.stream()
                        .map(plan -> new DailyNotePlan(dailyNote, plan))
                        .toList();
                dailyNotePlanRepository.saveAll(dailyNotePlans);

                allIncompletePlanResponses = allIncompletePlans.stream()
                        .map(PlanResponse::from)
                        .toList();
            } else {
                allIncompletePlanResponses = List.of();
            }
        } else {
            dailyNote = todayNote.get();
            List<Plan> allIncompletePlans = dailyNotePlanRepository.findAllIncompletePlans(dailyNote.getId());

            allIncompletePlanResponses = allIncompletePlans.stream()
                    .map(PlanResponse::from)
                    .toList();
        }

        return DailyNoteDetailResponse.from(dailyNote, allIncompletePlanResponses);
    }

    @Transactional(readOnly = true)
    public List<DailyNoteSimpleResponse> findAllDailyNotesByAuthorId(final Long authorId) {
        List<DailyNote> allDailyNotes = dailyNoteRepository.findAllByAuthorId(authorId);

        return allDailyNotes.stream()
                .map(DailyNoteSimpleResponse::from)
                .toList();
    }
}
