package com.example.search.chat;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Hybrid Search에서 keyword 검색에 사용할 후보를 추출한다.
 * 현재는 외부 LLM 호출 없이 가벼운 규칙 기반으로 동작하며,
 * 형태소 분석기나 LLM 기반 추출기로 교체할 수 있는 경계 역할을 한다.
 */
@Component
public class KeywordExtractor {

    private static final int MAX_KEYWORDS = 5;
    private static final int MIN_KEYWORD_LENGTH = 2;

    public List<String> extract(String question) {
        if (question == null || question.isBlank()) {
            return List.of();
        }
        return Arrays.stream(question.split("\\s+"))
                .map(String::strip)
                .map(this::removePunctuation)
                .filter(keyword -> keyword.length() >= MIN_KEYWORD_LENGTH)
                .distinct()
                .limit(MAX_KEYWORDS)
                .toList();
    }

    private String removePunctuation(String value) {
        return value.replaceAll("^[\\p{Punct}]+|[\\p{Punct}]+$", "");
    }
}
