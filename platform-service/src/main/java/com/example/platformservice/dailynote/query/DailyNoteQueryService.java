package com.example.platformservice.dailynote.query;

import com.example.platformservice.dailynote.command.domain.DailyNote;
import com.example.platformservice.dailynote.command.domain.DailyNoteJdbcRepository;
import com.example.platformservice.dailynote.command.domain.DailyNoteRepository;
import com.example.platformservice.dailynote.query.response.DailyNoteResponse;
import com.example.platformservice.dailynote.query.response.TomorrowTodo;
import com.example.platformservice.member.domain.Member;
import com.example.platformservice.member.domain.value.DayStartTime;
import com.example.platformservice.member.infra.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;

import static com.example.platformservice.Const.NO_USER_MESSAGE;

@RequiredArgsConstructor
@Service
public class DailyNoteQueryService {

    private static final String CANNOT_FIND_DAILY_NOTE = "DailyNote 를 찾을 수 없습니다";

    private final DailyNoteRepository dailyNoteRepository;
    private final DailyNoteJdbcRepository dailyNoteJdbcRepository;

    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public DailyNoteResponse findDailyNoteById(
            final Long authorId,
            final Long dailyNoteId
    ) {
        DailyNote dailyNote = dailyNoteRepository.findById(dailyNoteId)
                .orElseThrow(() -> new NoSuchElementException(CANNOT_FIND_DAILY_NOTE));

        if (!dailyNote.getAuthorId().equals(authorId)) {
            throw new IllegalArgumentException("조회가 허용되지 않았습니다");
        }

        return new DailyNoteResponse(
                dailyNote.getId(),
                dailyNote.getTodayTodo(),
                dailyNote.getTomorrowTodo(),
                dailyNote.getMemo()
        );
    }

    @Transactional
    public DailyNoteResponse getTodayDailyNote(final Long authorId) {
        Member member = memberRepository.findById(authorId)
                .orElseThrow(() -> new NoSuchElementException(NO_USER_MESSAGE));

        DayStartTime dayStartTime = member.getDayStartTime();
        LocalTime criteriaTime = LocalTime.of(dayStartTime.getHour(), dayStartTime.getMinute(), 0);

        LocalTime nowLocalTime = LocalTime.now();
        LocalDate calculatedDate = calculateDate(criteriaTime, nowLocalTime);
        LocalDateTime logicalNowDateTime = LocalDateTime.of(calculatedDate, criteriaTime);

        DailyNote dailyNote = dailyNoteRepository.findFirstByCreatedAtBetween(logicalNowDateTime, logicalNowDateTime.plusDays(1))
                .orElseGet(() -> {
                    String needTodo = dailyNoteRepository.findLatestTomorrowTodoWithin3days(authorId, logicalNowDateTime.minusDays(3))
                            .orElse("");

                    DailyNote todayDailyNote = new DailyNote(authorId, needTodo);
                    dailyNoteRepository.save(todayDailyNote);
                    return todayDailyNote;
                });

        return new DailyNoteResponse(
                dailyNote.getId(),
                dailyNote.getTodayTodo(),
                dailyNote.getTomorrowTodo(),
                dailyNote.getMemo()
        );
    }

    private LocalDate calculateDate(final LocalTime criteriaTime, final LocalTime nowLocalTime) {
        if (nowLocalTime.isBefore(criteriaTime)) {
            return LocalDate.now().minusDays(1);
        }
        return LocalDate.now();
    }

    @Transactional(readOnly = true)
    public List<DailyNoteResponse> findAllDailyNotesByAuthorId(final Long authorId) {
        List<DailyNote> allDailyNotes = dailyNoteRepository.findAllByAuthorId(authorId);

        return allDailyNotes.stream()
                .map(dailyNote -> new DailyNoteResponse(
                        dailyNote.getId(),
                        dailyNote.getTodayTodo(),
                        dailyNote.getTomorrowTodo(),
                        dailyNote.getMemo()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public TomorrowTodo findTomorrowTodo(final Long authorId) {
        LocalDateTime now = LocalDateTime.now();
        DailyNote dailyNote = dailyNoteJdbcRepository.findByAuthorIdAndCreatedAtBetween(authorId, now.minusDays(1), now)
                .orElseThrow(() -> new NoSuchElementException(CANNOT_FIND_DAILY_NOTE));

        return new TomorrowTodo(dailyNote.getAuthorId(), dailyNote.getTomorrowTodo());
    }


}
