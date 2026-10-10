package com.example.search.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OpenAiSearchClient {
    // Must match the Worker's embedding contract and PostgreSQL vector(1536) columns.
    private static final int EMBEDDING_DIMENSIONS = 1536;
    private static final int MAX_KEYWORDS = 5;
    private static final Pattern INPUT_PLACEHOLDER = Pattern.compile("\\{(context|question)}");
    private final ObjectMapper mapper;
    private final RestClient client;
    private final RestClient keywordClient;

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
    @Value("${openai.chat.input-template}")
    private String inputTemplate;
    @Value("${openai.keywords.instructions}")
    private String keywordInstructions;

    public OpenAiSearchClient(ObjectMapper mapper) {
        this.mapper = mapper;
        this.client = RestClient.builder().baseUrl("https://api.openai.com/v1").build();
        var keywordRequests = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)).build());
        keywordRequests.setReadTimeout(Duration.ofSeconds(10));
        this.keywordClient = RestClient.builder().baseUrl("https://api.openai.com/v1")
                .requestFactory(keywordRequests).build();
    }

    public String embeddingModel() {
        return embeddingModel;
    }

    /** Extracts search terms only; the original question is still used for semantic retrieval. */
    public List<String> extractKeywords(String question) {
        if (question == null || question.isBlank()) {
            return List.of();
        }
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of("keywords", Map.of(
                        "type", "array", "items", Map.of("type", "string"), "maxItems", MAX_KEYWORDS)),
                "required", List.of("keywords"),
                "additionalProperties", false);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", chatModel);
        body.put("instructions", keywordInstructions);
        body.put("input", question);
        body.put("text", Map.of("format", Map.of(
                "type", "json_schema", "name", "search_keywords", "strict", true, "schema", schema)));
        body.put("max_output_tokens", maxOutputTokens);
        body.put("store", false);

        JsonNode response = post(keywordClient, "/responses", body);
        if (response == null || !"completed".equals(response.path("status").asText())) {
            throw new IllegalStateException("키워드 추출 응답이 완료되지 않았습니다.");
        }
        for (JsonNode output : response.path("output")) {
            for (JsonNode content : output.path("content")) {
                if ("refusal".equals(content.path("type").asText())) {
                    throw new IllegalStateException("키워드 추출 요청이 거부되었습니다.");
                }
            }
        }
        StringBuilder text = new StringBuilder();
        collectOutputText(response.path("output"), text);
        if (text.isEmpty()) {
            throw new IllegalStateException("키워드 추출 응답이 비어 있습니다.");
        }
        JsonNode result;
        try {
            result = mapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readTree(text.toString());
        } catch (JsonProcessingException invalidJson) {
            throw new IllegalStateException("키워드 추출 응답이 올바른 JSON이 아닙니다.");
        }
        JsonNode keywords = result == null ? null : result.get("keywords");
        if (result == null || !result.isObject() || result.size() != 1
                || keywords == null || !keywords.isArray() || keywords.size() > MAX_KEYWORDS) {
            throw new IllegalStateException("키워드 추출 응답 형식이 올바르지 않습니다.");
        }
        List<String> extracted = new ArrayList<>();
        for (JsonNode keyword : keywords) {
            if (!keyword.isTextual()) {
                throw new IllegalStateException("키워드는 문자열이어야 합니다.");
            }
            extracted.add(keyword.asText());
        }
        return List.copyOf(extracted);
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
        body.put("input", formatChatInput(question, context));
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
        body.put("input", formatChatInput(question, context));
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

    private String formatChatInput(String question, String context) {
        // Substitute only template placeholders; user content must remain unchanged.
        return INPUT_PLACEHOLDER.matcher(inputTemplate).replaceAll(match ->
                Matcher.quoteReplacement("context".equals(match.group(1)) ? context : question));
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
        return post(client, uri, body);
    }

    private JsonNode post(RestClient requestClient, String uri, Object body) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY가 설정되지 않았습니다.");
        }
        return requestClient.post()
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
