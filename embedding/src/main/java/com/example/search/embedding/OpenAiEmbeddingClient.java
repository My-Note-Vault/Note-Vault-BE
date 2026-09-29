package com.example.search.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.time.Duration;

@Component
public class OpenAiEmbeddingClient implements EmbeddingClient {
    private static final int BATCH_SIZE = 64;
    private final RestClient client;

    @Value("${openai.api-key:}")
    private String apiKey;
    @Value("${openai.embedding.model:text-embedding-3-small}")
    private String embeddingModel;

    public OpenAiEmbeddingClient() {
        SimpleClientHttpRequestFactory requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(Duration.ofSeconds(5));
        requests.setReadTimeout(Duration.ofSeconds(60));
        this.client = RestClient.builder().baseUrl("https://api.openai.com/v1")
                .requestFactory(requests).build();
    }

    @Override
    public String embeddingModel() {
        return embeddingModel;
    }

    @Override
    public List<float[]> embed(List<String> input) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY가 설정되지 않았습니다.");
        }
        List<float[]> result = new ArrayList<>(input.size());
        for (int start = 0; start < input.size(); start += BATCH_SIZE) {
            if (Thread.currentThread().isInterrupted()) {
                throw new IllegalStateException("Embedding was interrupted");
            }
            result.addAll(embedBatch(input.subList(start, Math.min(start + BATCH_SIZE, input.size()))));
        }
        return List.copyOf(result);
    }

    private List<float[]> embedBatch(List<String> input) {
        JsonNode response = client.post()
                .uri("/embeddings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("model", embeddingModel, "input", input, "dimensions", DIMENSIONS))
                .retrieve()
                .body(JsonNode.class);
        if (response == null || !response.path("data").isArray()
                || response.path("data").size() != input.size()) {
            throw new IllegalStateException("Embedding response count does not match the input");
        }
        List<JsonNode> rows = new ArrayList<>();
        response.path("data").forEach(rows::add);
        rows.sort(Comparator.comparingInt(row -> row.path("index").asInt()));
        List<float[]> result = new ArrayList<>(rows.size());
        for (int index = 0; index < rows.size(); index++) {
            JsonNode row = rows.get(index);
            JsonNode vector = row.path("embedding");
            if (!row.path("index").isIntegralNumber() || row.path("index").asInt() != index
                    || !vector.isArray() || vector.size() != DIMENSIONS) {
                throw new IllegalStateException("Invalid embedding index or dimensions; expected " + DIMENSIONS);
            }
            float[] values = new float[DIMENSIONS];
            boolean nonZero = false;
            for (int dimension = 0; dimension < DIMENSIONS; dimension++) {
                JsonNode value = vector.get(dimension);
                if (!value.isNumber() || !Float.isFinite(value.floatValue())) {
                    throw new IllegalStateException("Invalid embedding vector value");
                }
                values[dimension] = value.floatValue();
                nonZero |= values[dimension] != 0;
            }
            if (!nonZero) throw new IllegalStateException("Embedding must not be a zero vector");
            result.add(values);
        }
        return result;
    }
}
