package com.example.workspace.workspace.command.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkSpaceRepository extends JpaRepository<WorkSpace, Long> {

    List<WorkSpace> findAllByCreatorId(Long authorId);
}
