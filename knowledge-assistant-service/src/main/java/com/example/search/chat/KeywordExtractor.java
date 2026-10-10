package com.example.search.chat;

import com.example.search.infrastructure.OpenAiSearchClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** OpenAI extracts at most five search terms; request failures fall back to local rules. */
@Component
public class KeywordExtractor {

    private static final Logger log = LoggerFactory.getLogger(KeywordExtractor.class);
    private static final int MAX_KEYWORDS = 5;
    private static final int MIN_KEYWORD_LENGTH = 2;
    private final OpenAiSearchClient openAi;

    public KeywordExtractor(OpenAiSearchClient openAi) {
        this.openAi = openAi;
    }

    public List<String> extract(String question) {
        if (question == null || question.isBlank()) {
            return List.of();
        }
        long started = System.nanoTime();
        try {
            List<String> keywords = normalize(openAi.extractKeywords(question));
            log.info("Search keywords extracted: source=OPENAI, count={}, durationMs={}",
                    keywords.size(), elapsedMillis(started));
            log.debug("Search keywords: source=OPENAI, keywords={}", keywords);
            return keywords;
        } catch (RuntimeException failure) {
            List<String> keywords = extractWithRules(question);
            // Upstream exception messages can contain response bodies; log only the failure type.
            log.warn("Search keywords extracted: source=RULES, count={}, durationMs={}, failure={}",
                    keywords.size(), elapsedMillis(started), failure.getClass().getSimpleName());
            log.debug("Search keywords: source=RULES, keywords={}", keywords);
            return keywords;
        }
    }

    private List<String> normalize(List<String> extracted) {
        var unique = new LinkedHashMap<String, String>();
        for (String value : extracted) {
            String keyword = Normalizer.normalize(value, Normalizer.Form.NFC)
                    .strip().replaceAll("\\s+", " ");
            if (!keyword.isEmpty()) {
                unique.putIfAbsent(keyword.toLowerCase(Locale.ROOT), keyword);
            }
            if (unique.size() == MAX_KEYWORDS) break;
        }
        return List.copyOf(unique.values());
    }

    private List<String> extractWithRules(String question) {
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

    private long elapsedMillis(long started) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }
}
