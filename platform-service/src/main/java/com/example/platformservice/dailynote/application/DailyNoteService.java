package com.example.platformservice.dailynote.application;

import com.example.common.file.image.ImageUtils;
import com.example.common.exception.ConflictException;
import com.example.platformservice.dailynote.application.response.DailyNoteDetailResponse;
import com.example.platformservice.dailynote.application.response.DailyNoteFolderResponse;
import com.example.platformservice.dailynote.application.response.DailyNoteListResponse;
import com.example.platformservice.dailynote.application.response.DailyNoteSimpleResponse;
import com.example.platformservice.dailynote.application.response.PlanResponse;
import com.example.platformservice.dailynote.domain.*;
import com.example.platformservice.member.domain.Member;
import com.example.platformservice.member.domain.value.DayStartTime;
import com.example.platformservice.member.infra.MemberRepository;
import com.example.search.sync.SearchSyncRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import static com.example.platformservice.PlatformConst.*;

@RequiredArgsConstructor
@Service
public class DailyNoteService {

    private final DailyNoteRepository dailyNoteRepository;
    private final DailyNoteFolderRepository dailyNoteFolderRepository;
    private final PlanRepository planRepository;
    private final DailyNotePlanRepository dailyNotePlanRepository;

    private final MemberRepository memberRepository;
    private final ImageUtils imageUtils;
    private final SearchSyncRecorder searchSyncRecorder;

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
        searchSyncRecorder.refreshDailyNote(dailyNoteId, dailyNote.getContentRevision());
        return plan.getId();
    }

    @Transactional
    public void editPlan(
            final Long authorId,
            final Long dailyNoteId,
            final Long planId,
            final Type type,
            final String content,
            final Boolean isDone
    ) {
        DailyNote dailyNote = dailyNoteRepository.findById(dailyNoteId)
                .orElseThrow(() -> new NoSuchElementException(NO_DAILY_NOTE_MESSAGE));
        if (!dailyNote.getAuthorId().equals(authorId)) {
            throw new IllegalArgumentException(ACCESS_DENIED);
        }

        if (!dailyNotePlanRepository.existsByDailyNote_IdAndPlan_Id(dailyNoteId, planId)) {
            throw new NoSuchElementException(NO_PLAN_MESSAGE);
        }

        Plan plan = planRepository.findWithWriteLockById(planId)
                .orElseThrow(() -> new NoSuchElementException(NO_PLAN_MESSAGE));
        String oldContent = plan.getContent();
        plan.edit(type, content, isDone);
        if (!Objects.equals(oldContent, plan.getContent())) {
            recordPlanRefreshes(dailyNotePlanRepository.findSearchTargetsByPlanId(planId));
        }
    }

    @Transactional
    public void deletePlan(final Long authorId, final Long dailyNoteId, final Long planId) {
        DailyNote dailyNote = dailyNoteRepository.findById(dailyNoteId)
                .orElseThrow(() -> new NoSuchElementException(NO_DAILY_NOTE_MESSAGE));
        if (!dailyNote.getAuthorId().equals(authorId)) {
            throw new IllegalArgumentException(ACCESS_DENIED);
        }

        if (!dailyNotePlanRepository.existsByDailyNote_IdAndPlan_Id(dailyNoteId, planId)) {
            throw new NoSuchElementException(NO_PLAN_MESSAGE);
        }
        Plan plan = planRepository.findWithWriteLockById(planId)
                .orElseThrow(() -> new NoSuchElementException(NO_PLAN_MESSAGE));

        List<DailyNotePlanRepository.SearchTarget> targets =
                dailyNotePlanRepository.findSearchTargetsByPlanId(planId);
        dailyNotePlanRepository.deleteAllByPlan(plan);
        planRepository.delete(plan);
        recordPlanRefreshes(targets);
    }

    @Transactional
    public long editDailyNote(
            final Long authorId,
            final Long dailyNoteId,
            final String content,
            final Long expectedRevision
    ) {
        if (content == null) {
            throw new IllegalArgumentException("content는 필수입니다");
        }
        if (expectedRevision == null || expectedRevision < 0) {
            throw new IllegalArgumentException("expectedRevision은 0 이상이어야 합니다");
        }

        DailyNote dailyNote = dailyNoteRepository.findByIdAndAuthorId(dailyNoteId, authorId)
                .orElseThrow(() -> new NoSuchElementException(NO_DAILY_NOTE_MESSAGE));

        String oldContent = dailyNote.getContent();
        int updated = dailyNoteRepository.updateContentIfRevisionMatches(
                dailyNoteId,
                authorId,
                content,
                expectedRevision
        );
        if (updated == 0) {
            throw new ConflictException("DailyNote가 다른 곳에서 수정되었습니다");
        }
        imageUtils.deleteRemovedContentImages(oldContent, content);
        searchSyncRecorder.refreshDailyNote(dailyNoteId, expectedRevision + 1);
        return expectedRevision + 1;
    }

    @Transactional
    public void deleteDailyNote(final Long memberId, final Long dailyNoteId) {
        // The worker also locks the note before its links when verifying an indexing result.
        DailyNote dailyNote = dailyNoteRepository.findWithWriteLockById(dailyNoteId)
                .orElseThrow(() -> new NoSuchElementException(NO_DAILY_NOTE_MESSAGE));

        if (!dailyNote.getAuthorId().equals(memberId)) {
            throw new IllegalArgumentException(ACCESS_DENIED);
        }
        imageUtils.deleteAllContentImages(dailyNote.getContent());
        searchSyncRecorder.deleteDailyNote(dailyNoteId);
        dailyNotePlanRepository.deleteAllByDailyNote(dailyNote);
        dailyNoteRepository.delete(dailyNote);
    }

    @Transactional(readOnly = true)
    public DailyNoteDetailResponse findDailyNoteByDate(final Long authorId, final LocalDate date) {
        DailyNote dailyNote = dailyNoteRepository.findByAuthorIdAndLogicalDate(authorId, date)
                .orElseThrow(() -> new NoSuchElementException(NO_DAILY_NOTE_MESSAGE));

        List<PlanResponse> allIncompletePlans = dailyNotePlanRepository.findAllPlansByDailyNoteId(dailyNote.getId()).stream()
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

        List<PlanResponse> allIncompletePlans = dailyNotePlanRepository.findAllPlansByDailyNoteId(dailyNote.getId()).stream()
                .map(PlanResponse::from)
                .toList();
        return DailyNoteDetailResponse.from(dailyNote, allIncompletePlans);
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

            Optional<DailyNote> latestDailyNote = dailyNoteRepository.findLatestDailyNoteBefore(authorId, logicalToday);
            if (latestDailyNote.isPresent()) {
                Long latestDailyNoteId = latestDailyNote.get().getId();
                List<Plan> allIncompletePlans = planRepository.findIncompleteForCarryOver(latestDailyNoteId);

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
            searchSyncRecorder.refreshDailyNote(dailyNote.getId(), dailyNote.getContentRevision());
        } else {
            dailyNote = todayNote.get();
            List<Plan> allIncompletePlans = dailyNotePlanRepository.findAllPlansByDailyNoteId(dailyNote.getId());

            allIncompletePlanResponses = allIncompletePlans.stream()
                    .map(PlanResponse::from)
                    .toList();
        }

        return DailyNoteDetailResponse.from(dailyNote, allIncompletePlanResponses);
    }

    @Transactional(readOnly = true)
    public DailyNoteListResponse findAllDailyNotesByAuthorId(final Long authorId) {
        List<DailyNote> allDailyNotes = dailyNoteRepository.findAllByAuthorIdOrderByLogicalDateAsc(authorId);
        List<DailyNoteSimpleResponse> notes = allDailyNotes.stream()
                .map(DailyNoteSimpleResponse::from)
                .toList();
        List<DailyNoteFolderResponse> folders = dailyNoteFolderRepository.findAllByAuthorIdOrderByNameAsc(authorId).stream()
                .map(DailyNoteFolderResponse::from)
                .toList();
        return new DailyNoteListResponse(folders, notes);
    }

    @Transactional
    public void moveDailyNote(final Long authorId, final Long dailyNoteId, final Long folderId) {
        DailyNote dailyNote = dailyNoteRepository.findByIdAndAuthorId(dailyNoteId, authorId)
                .orElseThrow(() -> new NoSuchElementException(NO_DAILY_NOTE_MESSAGE));
        if (folderId != null) {
            dailyNoteFolderRepository.findByIdAndAuthorId(folderId, authorId)
                    .orElseThrow(() -> new NoSuchElementException("일치하는 DailyNote 폴더가 없습니다"));
        }
        dailyNote.moveToFolder(folderId);
    }

    @Transactional
    public void moveDailyNotes(final Long authorId, final List<Long> dailyNoteIds, final Long folderId) {
        if (dailyNoteIds == null || dailyNoteIds.isEmpty() || dailyNoteIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("이동할 DailyNote 를 선택해야 합니다");
        }
        if (folderId != null) {
            dailyNoteFolderRepository.findWithWriteLockByIdAndAuthorId(folderId, authorId)
                    .orElseThrow(() -> new NoSuchElementException("일치하는 DailyNote 폴더가 없습니다"));
        }
        List<DailyNote> notes = dailyNoteIds.stream().distinct().sorted().map(id ->
                dailyNoteRepository.findWithWriteLockById(id)
                        .filter(note -> note.getAuthorId().equals(authorId))
                        .orElseThrow(() -> new NoSuchElementException(NO_DAILY_NOTE_MESSAGE))
        ).toList();
        notes.forEach(note -> note.moveToFolder(folderId));
    }

    private void recordPlanRefreshes(List<DailyNotePlanRepository.SearchTarget> targets) {
        targets.forEach(target -> searchSyncRecorder.refreshDailyNote(
                target.getDailyNoteId(), target.getContentRevision()));
    }
}
