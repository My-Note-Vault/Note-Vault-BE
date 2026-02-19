package com.example.platformservice.dailynote.query;

import com.example.platformservice.dailynote.command.domain.DailyNote;
import com.example.platformservice.dailynote.command.domain.DailyNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class DailyNoteQueryService {

    private final DailyNoteRepository dailyNoteRepository;

    public DailyNote findDailyNoteById(
            final Long authorId,
            final Long dailyNoteId
    ) {
        DailyNote dailyNote = dailyNoteRepository.findById(dailyNoteId)
                .orElseThrow(() -> new NoSuchElementException("DailyNote 를 찾을 수 없습니다"));

        if (!dailyNote.getAuthorId().equals(authorId)) {
            throw new IllegalArgumentException("조회가 허용되지 않았습니다");
        }
        return dailyNote;
    }

    public List<DailyNote> findAllDailyNotesByAuthorId(final Long authorId) {
        return dailyNoteRepository.findAllByAuthorId(authorId);
    }


}
