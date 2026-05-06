package com.example.workspace.trivia.query;

import com.example.workspace.trivia.command.domain.Trivia;
import com.example.workspace.trivia.command.domain.TriviaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class TriviaQueryService {

    private final TriviaRepository triviaRepository;

    public Trivia findTriviaById(
            final Long authorId,
            final Long triviaId
    ) {
        Trivia trivia = triviaRepository.findById(triviaId)
                .orElseThrow(() -> new NoSuchElementException("DailyNote 를 찾을 수 없습니다"));

        if (!trivia.getAuthorId().equals(authorId)) {
            throw new IllegalArgumentException("조회가 허용되지 않았습니다");
        }
        return trivia;
    }

    public List<Trivia> findAllTriviaByAuthorId(final Long authorId) {
        return triviaRepository.findAllByAuthorId(authorId);
    }


}
