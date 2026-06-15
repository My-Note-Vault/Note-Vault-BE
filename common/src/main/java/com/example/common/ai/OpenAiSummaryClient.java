package com.example.common.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OpenAiSummaryClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final int maxOutputTokens;

    public OpenAiSummaryClient(
            @Value("${openai.api-key:}") final String apiKey,
            @Value("${openai.model:gpt-5.4-nano}") final String model,
            @Value("${openai.summary.max-output-tokens:700}") final int maxOutputTokens
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxOutputTokens = maxOutputTokens;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .build();
    }

    public String summarize(
            final String title,
            final String sectionTitle,
            final String content
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY가 설정되지 않았습니다.");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", buildPrompt(title, sectionTitle, content));
        body.put("max_output_tokens", maxOutputTokens);

        JsonNode response = restClient.post()
                .uri("/responses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        String summary = extractText(response);
        if (summary.isBlank()) {
            throw new IllegalStateException("AI 요약 응답이 비어 있습니다.");
        }
        return summary.trim();
    }

    private String buildPrompt(
            final String title,
            final String sectionTitle,
            final String content
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("""
                너는 마크다운 문서를 요약하는 한국어 어시스턴트다.

                규칙:
                - 원문에 없는 내용을 추측하지 않는다.
                - 핵심 내용을 5개 이내 bullet로 요약한다.
                - 할 일, 결정사항, 중요한 메모가 있으면 별도 bullet에 포함한다.
                - 출력은 한국어 마크다운만 사용한다.
                - 불필요한 인사말이나 설명은 쓰지 않는다.

                """);

        appendIfPresent(prompt, "문서 제목", title);
        appendIfPresent(prompt, "섹션 제목", sectionTitle);
        prompt.append("원문:\n");
        prompt.append(content);
        return prompt.toString();
    }

    private void appendIfPresent(final StringBuilder prompt, final String label, final String value) {
        if (value != null && !value.isBlank()) {
            prompt.append(label).append(": ").append(value.trim()).append("\n");
        }
    }

    private String extractText(final JsonNode node) {
        if (node == null) {
            return "";
        }

        JsonNode outputText = node.get("output_text");
        if (outputText != null && outputText.isTextual()) {
            return outputText.asText();
        }

        StringBuilder text = new StringBuilder();
        collectOutputText(node, text);
        return text.toString();
    }

    private void collectOutputText(final JsonNode node, final StringBuilder text) {
        if (node == null) {
            return;
        }

        if (node.isObject()) {
            JsonNode type = node.get("type");
            JsonNode value = node.get("text");
            if (type != null && "output_text".equals(type.asText()) && value != null && value.isTextual()) {
                if (!text.isEmpty()) {
                    text.append("\n");
                }
                text.append(value.asText());
                return;
            }
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                collectOutputText(child, text);
            }
            return;
        }

        if (node.isObject()) {
            Iterator<JsonNode> children = node.elements();
            while (children.hasNext()) {
                collectOutputText(children.next(), text);
            }
        }
    }
}
