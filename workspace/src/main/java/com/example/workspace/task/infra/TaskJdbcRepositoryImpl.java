package com.example.workspace.task.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class TaskJdbcRepositoryImpl implements TaskJdbcRepository {

    private final JdbcTemplate jdbcTemplate;


}
