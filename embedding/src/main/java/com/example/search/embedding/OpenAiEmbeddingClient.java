package com.example.search.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiEmbeddingClient implements EmbeddingClient {
    private final ObjectMapper mapper;
    private final RestClient client;

    @Value("${openai.api-key:}")
    private String apiKey;
    @Value("${openai.embedding.model:text-embedding-3-small}")
    private String embeddingModel;

    public OpenAiEmbeddingClient(ObjectMapper mapper) {
        this.mapper = mapper;
        this.client = RestClient.builder().baseUrl("https://api.openai.com/v1").build();
    }

    @Override
    public String embeddingModel() {
        return embeddingModel;
    }

    @Override
    public List<String> embed(List<String> input) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY가 설정되지 않았습니다.");
        }
        JsonNode response = client.post()
                .uri("/embeddings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("model", embeddingModel, "input", input))
                .retrieve()
                .body(JsonNode.class);
        List<JsonNode> rows = new ArrayList<>();
        response.path("data").forEach(rows::add);
        rows.sort(Comparator.comparingInt(row -> row.path("index").asInt()));
        try {
            List<String> result = new ArrayList<>();
            for (JsonNode row : rows) {
                result.add(mapper.writeValueAsString(row.path("embedding")));
            }
            return result;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
