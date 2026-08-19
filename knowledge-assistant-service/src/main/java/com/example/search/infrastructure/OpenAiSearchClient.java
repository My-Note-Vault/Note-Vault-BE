package com.example.search.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiSearchClient {
    private final ObjectMapper mapper;
    private final RestClient client;

    @Value("${openai.api-key:}")
    private String apiKey;
    @Value("${openai.embedding.model:text-embedding-3-small}")
    private String embeddingModel;
    @Value("${openai.chat.model:gpt-5.4-mini}")
    private String chatModel;
    @Value("${openai.chat.max-output-tokens:1200}")
    private int maxOutputTokens;
    @Value("${openai.chat.instructions}")
    private String instructions;

    public OpenAiSearchClient(ObjectMapper mapper) {
        this.mapper = mapper;
        this.client = RestClient.builder().baseUrl("https://api.openai.com/v1").build();
    }

    public String embeddingModel() {
        return embeddingModel;
    }

    public List<String> embed(List<String> input) {
        JsonNode response = post("/embeddings", Map.of("model", embeddingModel, "input", input));
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

    public String answer(String question, String context) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", chatModel);
        body.put("instructions", instructions);
        body.put("input", "문맥:\n" + context + "\n질문:\n" + question);
        body.put("max_output_tokens", maxOutputTokens);
        body.put("store", false);

        JsonNode response = post("/responses", body);
        JsonNode direct = response.get("output_text");
        if (direct != null && direct.isTextual()) {
            return direct.asText();
        }
        StringBuilder text = new StringBuilder();
        collectOutputText(response, text);
        if (text.isEmpty()) {
            throw new IllegalStateException("챗봇 응답이 비어 있습니다.");
        }
        return text.toString();
    }

    private JsonNode post(String uri, Object body) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY가 설정되지 않았습니다.");
        }
        return client.post()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
    }

    private void collectOutputText(JsonNode node, StringBuilder result) {
        if (node == null) {
            return;
        }
        if (node.isObject()
                && "output_text".equals(node.path("type").asText())
                && node.path("text").isTextual()) {
            if (!result.isEmpty()) {
                result.append('\n');
            }
            result.append(node.path("text").asText());
            return;
        }
        if (node.isContainerNode()) {
            node.elements().forEachRemaining(child -> collectOutputText(child, result));
        }
    }
}
