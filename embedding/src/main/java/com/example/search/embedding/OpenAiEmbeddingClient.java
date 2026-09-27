package com.example.search.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper mapper;
    private final RestClient client;

    @Value("${openai.api-key:}")
    private String apiKey;
    @Value("${openai.embedding.model:text-embedding-3-small}")
    private String embeddingModel;

    public OpenAiEmbeddingClient(ObjectMapper mapper) {
        this.mapper = mapper;
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
    public List<String> embed(List<String> input) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY가 설정되지 않았습니다.");
        }
        List<String> result = new ArrayList<>(input.size());
        for (int start = 0; start < input.size(); start += BATCH_SIZE) {
            if (Thread.currentThread().isInterrupted()) {
                throw new IllegalStateException("Embedding was interrupted");
            }
            result.addAll(embedBatch(input.subList(start, Math.min(start + BATCH_SIZE, input.size()))));
        }
        return List.copyOf(result);
    }

    private List<String> embedBatch(List<String> input) {
        JsonNode response = client.post()
                .uri("/embeddings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("model", embeddingModel, "input", input))
                .retrieve()
                .body(JsonNode.class);
        if (response == null || !response.path("data").isArray()
                || response.path("data").size() != input.size()) {
            throw new IllegalStateException("Embedding response count does not match the input");
        }
        List<JsonNode> rows = new ArrayList<>();
        response.path("data").forEach(rows::add);
        rows.sort(Comparator.comparingInt(row -> row.path("index").asInt()));
        try {
            List<String> result = new ArrayList<>();
            int dimensions = -1;
            for (int index = 0; index < rows.size(); index++) {
                JsonNode row = rows.get(index);
                JsonNode vector = row.path("embedding");
                if (!row.path("index").isIntegralNumber() || row.path("index").asInt() != index
                        || !vector.isArray() || vector.isEmpty()
                        || (dimensions != -1 && dimensions != vector.size())) {
                    throw new IllegalStateException("Invalid embedding index or dimensions");
                }
                for (JsonNode value : vector) {
                    if (!value.isNumber() || !Double.isFinite(value.asDouble())) {
                        throw new IllegalStateException("Invalid embedding vector value");
                    }
                }
                dimensions = vector.size();
                result.add(mapper.writeValueAsString(vector));
            }
            return result;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
