package com.example.search.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class OpenAiSearchClient {
    // Must match the Worker's embedding contract and PostgreSQL vector(1536) columns.
    private static final int EMBEDDING_DIMENSIONS = 1536;
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

    /** Embeds a single search question; document indexing belongs to the worker. */
    public String embedQuestion(String question) {
        JsonNode response = post("/embeddings", Map.of("model", embeddingModel, "input", question,
                "dimensions", EMBEDDING_DIMENSIONS));
        JsonNode data = response == null ? null : response.get("data");
        if (data == null || !data.isArray() || data.size() != 1
                || !data.get(0).path("embedding").isArray()
                || data.get(0).path("embedding").size() != EMBEDDING_DIMENSIONS) {
            throw new IllegalStateException("질문 임베딩 응답이 올바르지 않습니다.");
        }
        JsonNode vector = data.get(0).get("embedding");
        boolean nonZero = false;
        for (JsonNode value : vector) {
            if (!value.isNumber() || !Float.isFinite(value.floatValue())) {
                throw new IllegalStateException("질문 임베딩에 유효하지 않은 숫자가 있습니다.");
            }
            nonZero |= value.floatValue() != 0;
        }
        if (!nonZero) throw new IllegalStateException("질문 임베딩은 영벡터일 수 없습니다.");
        // Bound as a query parameter and cast to vector in SQL; never stored as TEXT.
        return vector.toString();
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

    public void streamAnswer(String question, String context, Consumer<String> onDelta) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY가 설정되지 않았습니다.");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", chatModel);
        body.put("instructions", instructions);
        body.put("input", "문맥:\n" + context + "\n질문:\n" + question);
        body.put("max_output_tokens", maxOutputTokens);
        body.put("store", false);
        body.put("stream", true);

        client.post()
                .uri("/responses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .body(body)
                .exchange((request, response) -> {
                    if (response.getStatusCode().isError()) {
                        String error = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        throw new IllegalStateException("OpenAI 응답 실패: " + error);
                    }

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                            response.getBody(), StandardCharsets.UTF_8))) {
                        StringBuilder eventData = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.isEmpty()) {
                                handleStreamEvent(eventData, onDelta);
                                eventData.setLength(0);
                            } else if (line.startsWith("data:")) {
                                if (!eventData.isEmpty()) {
                                    eventData.append('\n');
                                }
                                eventData.append(line.substring(5).stripLeading());
                            }
                        }
                        handleStreamEvent(eventData, onDelta);
                    }
                    return null;
                });
    }

    private void handleStreamEvent(StringBuilder eventData, Consumer<String> onDelta) {
        if (eventData.isEmpty() || "[DONE]".contentEquals(eventData)) {
            return;
        }
        try {
            JsonNode event = mapper.readTree(eventData.toString());
            String type = event.path("type").asText();
            if ("response.output_text.delta".equals(type)) {
                String delta = event.path("delta").asText();
                if (!delta.isEmpty()) {
                    onDelta.accept(delta);
                }
            } else if ("error".equals(type) || "response.failed".equals(type)) {
                throw new IllegalStateException("OpenAI 스트림 실패: " + event);
            }
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("OpenAI 스트림을 해석하지 못했습니다.", exception);
        }
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
