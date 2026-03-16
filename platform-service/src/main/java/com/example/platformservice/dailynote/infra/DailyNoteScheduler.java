package com.example.platformservice.dailynote.infra;

import com.example.platformservice.dailynote.command.application.DailyNoteCommandService;
import com.example.platformservice.dailynote.query.DailyNoteQueryService;
import com.example.platformservice.dailynote.query.TomorrowTodo;
import com.example.platformservice.member.domain.value.DayStartTime;
import com.example.platformservice.member.application.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Component
@RequiredArgsConstructor
public class DailyNoteScheduler {

    private static final int ONE_MINUTE = 60000;

    private final MemberService memberService;

    private final DailyNoteCommandService dailyNoteCommandService;
    private final DailyNoteQueryService dailyNoteQueryService;

    @Transactional
    @Scheduled(fixedDelay = ONE_MINUTE)
    public void createDailyNote() {
        DayStartTime now = DayStartTime.now();
        List<Long> memberIds = memberService.findAllMembersByDayStartTime(now);

        memberIds.forEach(memberId -> {
            try {
                TomorrowTodo tomorrowTodo = dailyNoteQueryService.findTomorrowTodo(memberId);
                dailyNoteCommandService.createTomorrowDailyNote(memberId, tomorrowTodo.getTodo());
            } catch (NoSuchElementException e) {
                dailyNoteCommandService.createTomorrowDailyNote(memberId, "");
            }
        });
    }

}
