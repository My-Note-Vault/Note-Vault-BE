package com.example.noteservice.trivia.query;

import com.example.noteservice.trivia.command.domain.Trivia;
import com.example.noteservice.trivia.command.domain.TriviaRepository;
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
                .orElseThrow(() -> new NoSuchElementException("Trivia 를 찾을 수 없습니다"));

        if (!trivia.getAuthorId().equals(authorId)) {
            throw new IllegalArgumentException("조회가 허용되지 않았습니다");
        }
        return trivia;
    }

    public List<Trivia> findAllTriviaByAuthorId(final Long authorId) {
        return triviaRepository.findAllByAuthorId(authorId);
    }


}
