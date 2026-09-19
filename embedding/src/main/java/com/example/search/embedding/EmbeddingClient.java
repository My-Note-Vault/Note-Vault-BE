package com.example.search.embedding;

import java.util.List;

public interface EmbeddingClient {
    String embeddingModel();

    List<String> embed(List<String> input);
}
