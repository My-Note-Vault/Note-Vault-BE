package com.example.search.content;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentTitleEmbeddingRepository
        extends JpaRepository<ContentTitleEmbedding, ContentTitleEmbedding.Id> {
}
