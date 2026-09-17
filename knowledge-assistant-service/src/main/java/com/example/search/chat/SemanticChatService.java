package com.example.search.chat;

import com.example.search.content.ContentChunk;
import com.example.search.content.ContentChunkRepository;
import com.example.search.infrastructure.OpenAiSearchClient;
import com.notevault.workspace.api.search.KeywordSearchReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class SemanticChatService {
    private final ContentChunkRepository chunks;
    private final ObjectMapper mapper;
    private final OpenAiSearchClient openAi;
    private final KeywordSearchReader keywordSearch;
    private final KeywordExtractor keywordExtractor;

    @Value("${openai.chat.top-k:6}")
    private int topK;
    @Value("${openai.chat.min-similarity:0.3}")
    private double minSimilarity;
    @Value("${openai.chat.max-candidates:5000}")
    private int maxCandidates;
    @Value("${openai.chat.semantic-weight:0.7}")
    private double semanticWeight;
    @Value("${openai.chat.keyword-weight:0.3}")
    private double keywordWeight;
    @Value("${openai.chat.keyword-top-k:20}")
    private int keywordTopK;

    public ChatResult chat(Long memberId, String question) {
        ChatPreparation preparation = prepare(memberId, question);
        if (!preparation.hasContext()) {
            return new ChatResult("NO_CONTEXT", "관련 문서 내용을 찾지 못했습니다.", List.of());
        }
        return new ChatResult("ANSWERED",
                openAi.answer(preparation.question(), preparation.context()), preparation.sources());
    }

    public ChatPreparation prepare(Long memberId, String question) {
        if (question == null || question.isBlank() || question.length() > 4000) {
            throw new IllegalArgumentException("질문은 1자 이상 4000자 이하여야 합니다.");
        }

        String normalizedQuestion = question.trim();
        double[] query = parse(openAi.embed(List.of(normalizedQuestion)).getFirst());
        List<ContentChunk> candidates = chunks.findAccessibleCandidates(
                        memberId, openAi.embeddingModel(), PageRequest.of(0, maxCandidates)).stream()
                .toList();
        Map<SourceKey, Double> keywordScores = keywordScores(memberId, normalizedQuestion);
        List<ScoredChunk> found = candidates.stream()
                .map(chunk -> {
                    double semantic = cosine(query, parse(chunk.getEmbedding()));
                    SourceKey key = new SourceKey(chunk.getSourceType().name(), chunk.getSourceId());
                    double keyword = keywordScores.getOrDefault(key, 0.0);
                    return new ScoredChunk(chunk, semantic, keyword,
                            semanticWeight * normalizeSemantic(semantic)
                                    + keywordWeight * keyword);
                })
                .filter(result -> result.semantic() >= minSimilarity || result.keyword() > 0)
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(topK)
                .toList();
        if (found.isEmpty()) {
            return new ChatPreparation(question.trim(), "", List.of());
        }

        StringBuilder context = new StringBuilder();
        List<Source> sources = new ArrayList<>();
        for (int index = 0; index < found.size(); index++) {
            ScoredChunk result = found.get(index);
            ContentChunk chunk = result.chunk();
            int number = index + 1;
            context.append('[').append(number).append("]\n문서: ")
                    .append(chunk.getSourceTitle()).append("\n내용:\n")
                    .append(chunk.getContent()).append("\n\n");
            sources.add(new Source(number, chunk.getId(), chunk.getSourceType().name(),
                    chunk.getResourceId(), chunk.getResourceType(), chunk.getSourceTitle(),
                    result.semantic(), excerpt(chunk.getContent())));
        }
        return new ChatPreparation(normalizedQuestion, context.toString(), sources);
    }

    public void streamAnswer(ChatPreparation preparation, java.util.function.Consumer<String> onDelta) {
        openAi.streamAnswer(preparation.question(), preparation.context(), onDelta);
    }

    private double[] parse(String json) {
        try {
            JsonNode node = mapper.readTree(json);
            double[] vector = new double[node.size()];
            for (int index = 0; index < vector.length; index++) {
                vector[index] = node.get(index).asDouble();
            }
            return vector;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private double cosine(double[] left, double[] right) {
        if (left.length != right.length) {
            return -1;
        }
        double dot = 0;
        double leftLength = 0;
        double rightLength = 0;
        for (int index = 0; index < left.length; index++) {
            dot += left[index] * right[index];
            leftLength += left[index] * left[index];
            rightLength += right[index] * right[index];
        }
        return leftLength == 0 || rightLength == 0
                ? -1
                : dot / (Math.sqrt(leftLength) * Math.sqrt(rightLength));
    }

    private String excerpt(String content) {
        return content.length() <= 300 ? content : content.substring(0, 300) + "…";
    }

    private Map<SourceKey, Double> keywordScores(Long memberId, String question) {
        Map<SourceKey, Double> scores = new HashMap<>();
        keywordSearch.search(memberId, keywordExtractor.extract(question), keywordTopK)
                .forEach(hit -> scores.merge(
                        new SourceKey(hit.sourceType().name(), hit.sourceId()), hit.score(), Math::max));
        return scores;
    }

    private double normalizeSemantic(double similarity) {
        if (similarity <= minSimilarity) return 0.0;
        return Math.min(1.0, (similarity - minSimilarity) / (1.0 - minSimilarity));
    }

    private record SourceKey(String sourceType, Long sourceId) {
    }

    private record ScoredChunk(ContentChunk chunk, double semantic, double keyword, double score) {
    }

    public record Source(int number, Long chunkId, String sourceType, Long resourceId,
                         String resourceType, String title, double similarity, String excerpt) {
    }

    public record ChatResult(String status, String answer, List<Source> sources) {
    }

    public record ChatPreparation(String question, String context, List<Source> sources) {
        public boolean hasContext() {
            return !sources.isEmpty();
        }
    }
}
