package com.example.workspace.trivia.command.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TriviaRepository extends JpaRepository<Trivia, Long> {

    List<Trivia> findAllByAuthorId(Long authorId);
}
