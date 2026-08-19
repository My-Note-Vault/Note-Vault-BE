package com.example.search.chat;

import com.example.search.content.ContentChunk;
import com.example.search.content.ContentChunkRepository;
import com.example.search.infrastructure.OpenAiSearchClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Service
public class SemanticChatService {
    private final ContentChunkRepository chunks;
    private final ObjectMapper mapper;
    private final OpenAiSearchClient openAi;

    @Value("${openai.chat.top-k:6}")
    private int topK;
    @Value("${openai.chat.min-similarity:0.3}")
    private double minSimilarity;
    @Value("${openai.chat.max-candidates:5000}")
    private int maxCandidates;

    public ChatResult chat(Long memberId, String question) {
        if (question == null || question.isBlank() || question.length() > 4000) {
            throw new IllegalArgumentException("질문은 1자 이상 4000자 이하여야 합니다.");
        }

        double[] query = parse(openAi.embed(List.of(question.trim())).getFirst());
        List<ScoredChunk> found = chunks.findAccessibleCandidates(
                        memberId, openAi.embeddingModel(), PageRequest.of(0, maxCandidates)).stream()
                .map(chunk -> new ScoredChunk(chunk, cosine(query, parse(chunk.getEmbedding()))))
                .filter(result -> result.similarity() >= minSimilarity)
                .sorted(Comparator.comparingDouble(ScoredChunk::similarity).reversed())
                .limit(topK)
                .toList();
        if (found.isEmpty()) {
            return new ChatResult("NO_CONTEXT", "관련 문서 내용을 찾지 못했습니다.", List.of());
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
                    result.similarity(), excerpt(chunk.getContent())));
        }
        return new ChatResult("ANSWERED", openAi.answer(question.trim(), context.toString()), sources);
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

    private record ScoredChunk(ContentChunk chunk, double similarity) {
    }

    public record Source(int number, Long chunkId, String sourceType, Long resourceId,
                         String resourceType, String title, double similarity, String excerpt) {
    }

    public record ChatResult(String status, String answer, List<Source> sources) {
    }
}
