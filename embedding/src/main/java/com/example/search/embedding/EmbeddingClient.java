package com.example.search.embedding;

import java.util.List;

public interface EmbeddingClient {
    int DIMENSIONS = 1536;

    String embeddingModel();

    List<float[]> embed(List<String> input);
}
