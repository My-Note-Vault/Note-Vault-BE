package com.example.noteservice.trivia.command.application;

import com.example.noteservice.trivia.command.domain.Trivia;
import com.example.noteservice.trivia.command.domain.TriviaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class TriviaCommandService {

    private final TriviaRepository triviaRepository;

    @Transactional
    public Long createTrivia(
            final Long memberId,
            final Long taskId,
            final String title,
            final String content,
            final Boolean isHidden
    ) {
        Trivia trivia = new Trivia(memberId, taskId, title, content, isHidden);
        triviaRepository.save(trivia);

        return trivia.getId();
    }


    @Transactional
    public void editTrivia(final Long memberId, final Long triviaId, final String title, final String content, final Boolean isHidden) {
        Trivia trivia = triviaRepository.findById(triviaId)
                .orElseThrow(() -> new NoSuchElementException("Trivia 를 찾을 수 없습니다"));

        trivia.edit(memberId, title, content, isHidden);
    }

    @Transactional
    public void deleteTrivia(final Long memberId, final Long triviaId) {
        Trivia trivia = triviaRepository.findById(triviaId)
                .orElseThrow(() -> new NoSuchElementException("Trivia 를 찾을 수 없습니다"));

        if (!trivia.getAuthorId().equals(memberId)) {
            throw new IllegalArgumentException("삭제할 권한이 없습니다.");
        }
        triviaRepository.delete(trivia);
    }
}
