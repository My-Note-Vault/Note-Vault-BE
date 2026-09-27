package com.example.search.content;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentSourceRepository extends Repository<DocumentSource, Long> {
    Optional<DocumentSource> findById(Long id);

    // FOR UPDATE also excludes FK inserts while a source is being verified.
    @Query(value = "SELECT * FROM document WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<DocumentSource> lockById(@Param("id") Long id);

    @Query(value = "SELECT id FROM document WHERE id > :after ORDER BY id LIMIT 256", nativeQuery = true)
    List<Long> findNextIds(@Param("after") long after);
}
