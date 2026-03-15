package com.example.workspace.unfolded.infra;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class UnfoldedNotesJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public void findAllUnfoldedNotes(final Long authorId) {

    }
}
