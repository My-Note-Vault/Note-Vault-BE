package com.example.platformservice.dailynote.command.application;

import com.example.platformservice.dailynote.command.domain.DailyNote;
import com.example.platformservice.dailynote.infra.DailyNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class DailyNoteCommandService {

    private final DailyNoteRepository dailyNoteRepository;

    //TODO: 이거 조금 이상한데?? 고쳐야 할 듯?
    @Transactional
    public Long createDailyNoteAtFirst(
            final Long memberId,
            final String todayTodoList,
            final String tomorrowTodoList,
            final String memo,
            final Boolean isCollapsed
    ) {
        List<DailyNote> todayDailyNote = dailyNoteRepository.findAllByCreatedAt(LocalDateTime.now());
        if (!todayDailyNote.isEmpty()) {
            return todayDailyNote.getFirst().getId();
        }

        // 여기부터가 노트 생성로직
        List<DailyNote> yesterdayDailyNote = dailyNoteRepository.findAllByCreatedAt(LocalDateTime.now().minusDays(1));
        if (yesterdayDailyNote.isEmpty()) {
            DailyNote dailyNote = new DailyNote(memberId, todayTodoList, tomorrowTodoList, memo, isCollapsed);
            dailyNoteRepository.save(dailyNote);
        }

        DailyNote yesterdayNote = yesterdayDailyNote.getFirst();
        DailyNote dailyNote = new DailyNote(memberId, yesterdayNote.getTomorrowTodo(), tomorrowTodoList, memo, isCollapsed);
        dailyNoteRepository.save(dailyNote);

        return dailyNote.getId();
    }

    @Transactional
    public Long createTomorrowDailyNote(final Long memberId, final String todayTodo) {
        DailyNote dailyNote = new DailyNote(memberId, todayTodo);
        dailyNoteRepository.save(dailyNote);
        return dailyNote.getId();
    }


    @Transactional
    public void editDailyNote(
            final Long memberId,
            final Long dailyNoteId,
            final String todayTodoList,
            final String tomorrowTodoList,
            final String memo,
            final Boolean isCollapsed
    ) {
        DailyNote dailyNote = dailyNoteRepository.findById(dailyNoteId)
                .orElseThrow(() -> new NoSuchElementException("DailyNote 를 찾을 수 없습니다"));

        dailyNote.edit(memberId, todayTodoList, tomorrowTodoList, memo, isCollapsed);
    }

    @Transactional
    public void deleteDailyNote(final Long memberId, final Long dailyNoteId) {
        DailyNote dailyNote = dailyNoteRepository.findById(dailyNoteId)
                .orElseThrow(() -> new NoSuchElementException("DailyNote 를 찾을 수 없습니다"));

        if (!dailyNote.getAuthorId().equals(memberId)) {
            throw new IllegalArgumentException("삭제할 권한이 없습니다.");
        }
        dailyNoteRepository.delete(dailyNote);
    }
}
