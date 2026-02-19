package com.example.noteservice.subtask.command.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubTaskRepository extends JpaRepository<SubTask, Long> {

    List<SubTask> findAllByAuthorId(Long authorId);
}
