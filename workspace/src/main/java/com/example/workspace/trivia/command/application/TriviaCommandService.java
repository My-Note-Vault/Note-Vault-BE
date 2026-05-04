package com.example.workspace.trivia.command.application;

import com.example.workspace.trivia.command.domain.Trivia;
import com.example.workspace.trivia.command.domain.TriviaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class TriviaCommandService {

    private final TriviaRepository triviaRepository;

    @Transactional
    public Long createTrivia(final Long authorId, final Long subTaskId) {
        Trivia trivia = new Trivia(authorId, subTaskId);
        triviaRepository.save(trivia);

        return trivia.getId();
    }


    @Transactional
    public void editTrivia(final Long memberId, final Long triviaId, final Long subTaskId, final String title, final String content, final Boolean isPublic) {
        Trivia trivia = triviaRepository.findById(triviaId)
                .orElseThrow(() -> new NoSuchElementException("DailyNote 를 찾을 수 없습니다"));

        trivia.edit(memberId, subTaskId, title, content, isPublic);
        triviaRepository.save(trivia);
    }

    @Transactional
    public void deleteTrivia(final Long memberId, final Long triviaId) {
        Trivia trivia = triviaRepository.findById(triviaId)
                .orElseThrow(() -> new NoSuchElementException("DailyNote 를 찾을 수 없습니다"));

        if (!trivia.getAuthorId().equals(memberId)) {
            throw new IllegalArgumentException("삭제할 권한이 없습니다.");
        }
        triviaRepository.delete(trivia);
    }
}
