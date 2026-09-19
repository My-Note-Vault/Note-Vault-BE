package com.example.search.retrieval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notevault.workspace.api.search.KeywordSearchReader;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HybridSearch {
    private final IndexedChunkReader reader;
    private final KeywordSearchReader keywords;
    private final ObjectMapper mapper;

    @Value("${openai.chat.min-similarity:0.3}")
    private double minSimilarity;
    @Value("${openai.chat.title-min-similarity:0.3}")
    private double titleMinSimilarity;
    @Value("${openai.chat.rrf-rank-constant:60}")
    private int rankConstant;
    @Value("${openai.chat.title-semantic-weight:1.2}")
    private double titleSemanticWeight;
    @Value("${openai.chat.title-keyword-weight:1.2}")
    private double titleKeywordWeight;
    @Value("${openai.chat.body-semantic-weight:1.0}")
    private double bodySemanticWeight;
    @Value("${openai.chat.body-keyword-weight:1.0}")
    private double bodyKeywordWeight;
    @Value("${openai.chat.document-top-k:3}")
    private int documentTopK;
    @Value("${openai.chat.chunks-per-document:2}")
    private int chunksPerDocument;

    public List<Result> search(Long memberId, String question, List<String> terms,
                               String model, String questionEmbedding, int topK) {
        if (topK < 1 || documentTopK < 1 || chunksPerDocument < 1) {
            throw new IllegalArgumentException("Search result counts must be positive");
        }
        if (!Double.isFinite(minSimilarity) || minSimilarity < -1 || minSimilarity >= 1
                || !Double.isFinite(titleMinSimilarity) || titleMinSimilarity < -1 || titleMinSimilarity > 1) {
            throw new IllegalArgumentException("Invalid similarity threshold");
        }
        double[] query = parse(questionEmbedding);
        Map<SourceKey, Double> titleKeyword = new HashMap<>();
        Map<Long, Double> chunkKeyword = new HashMap<>();
        keywords.scanHybridMatches(memberId, question, terms, hit -> {
            if (hit.chunkId() == null) {
                titleKeyword.put(new SourceKey(hit.sourceType().name(), hit.sourceId()), hit.score());
            } else {
                chunkKeyword.put(hit.chunkId(), hit.score());
            }
        });

        Map<SourceKey, Double> titleSemantic = new HashMap<>();
        reader.scanAccessibleTitles(memberId, model, title -> {
            double similarity = cosine(query, parse(title.embedding()));
            if (similarity >= titleMinSimilarity) {
                titleSemantic.put(new SourceKey(title.sourceType(), title.sourceId()), similarity);
            }
        });

        Map<SourceKey, Double> bodySemantic = new HashMap<>();
        Map<SourceKey, Double> bodyKeyword = new HashMap<>();
        Map<SourceKey, List<Candidate>> bestChunks = new HashMap<>();
        Comparator<Candidate> chunkOrder = Comparator.comparingDouble(Candidate::score).reversed()
                .thenComparing(Comparator.comparingDouble(Candidate::semantic).reversed())
                .thenComparingLong(Candidate::id);

        // Every current chunk is visited. Retain IDs/scores only, not the full corpus or vectors.
        reader.scanAccessibleChunks(memberId, model, chunk -> {
            SourceKey source = new SourceKey(chunk.sourceType(), chunk.sourceId());
            Double matched = chunkKeyword.remove(chunk.id());
            double keyword = matched == null ? 0 : matched;
            double semantic = chunk.embedding() == null ? -1 : cosine(query, parse(chunk.embedding()));
            if (chunk.embedding() != null && semantic >= minSimilarity) {
                bodySemantic.merge(source, semantic, Math::max);
            }
            if (keyword > 0) bodyKeyword.merge(source, keyword, Math::max);
            double normalized = Math.max(0, Math.min(1, (semantic - minSimilarity) / (1 - minSimilarity)));
            double score = 0.7 * normalized + 0.3 * Math.min(1, keyword / 4.0);
            List<Candidate> best = bestChunks.computeIfAbsent(source, ignored -> new ArrayList<>());
            best.add(new Candidate(chunk.id(), semantic, score));
            best.sort(chunkOrder);
            if (best.size() > chunksPerDocument) best.removeLast();
        });

        // Title-only/empty documents cannot provide body context and must not displace usable documents.
        titleSemantic.keySet().retainAll(bestChunks.keySet());
        titleKeyword.keySet().retainAll(bestChunks.keySet());
        Map<SourceKey, Double> fused = WeightedRrf.fuse(List.of(
                new WeightedRrf.Ranking<>(titleSemantic, titleSemanticWeight),
                new WeightedRrf.Ranking<>(titleKeyword, titleKeywordWeight),
                new WeightedRrf.Ranking<>(bodySemantic, bodySemanticWeight),
                new WeightedRrf.Ranking<>(bodyKeyword, bodyKeywordWeight)), rankConstant);
        List<SourceKey> documents = fused.entrySet().stream()
                .sorted(Map.Entry.<SourceKey, Double>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().type())
                        .thenComparing(entry -> entry.getKey().id()))
                .limit(documentTopK).map(Map.Entry::getKey).toList();

        List<Candidate> selected = new ArrayList<>();
        for (int position = 0; position < chunksPerDocument && selected.size() < topK; position++) {
            for (SourceKey document : documents) {
                List<Candidate> best = bestChunks.get(document);
                if (position < best.size()) selected.add(best.get(position));
                if (selected.size() == topK) break;
            }
        }
        Map<Long, IndexedChunk> loaded = new HashMap<>();
        reader.findAccessibleByIds(memberId, model, selected.stream().map(Candidate::id).toList())
                .forEach(chunk -> loaded.put(chunk.id(), chunk));
        return selected.stream().filter(candidate -> loaded.containsKey(candidate.id()))
                .map(candidate -> new Result(loaded.get(candidate.id()), candidate.semantic())).toList();
    }

    private double[] parse(String json) {
        try {
            JsonNode node = mapper.readTree(json);
            if (node == null || !node.isArray() || node.isEmpty()) {
                throw new IllegalArgumentException("Embedding must be a non-empty numeric array");
            }
            double[] vector = new double[node.size()];
            for (int index = 0; index < vector.length; index++) {
                if (!node.get(index).isNumber() || !Double.isFinite(node.get(index).asDouble())) {
                    throw new IllegalArgumentException("Embedding contains a non-finite/non-numeric value");
                }
                vector[index] = node.get(index).asDouble();
            }
            return vector;
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid embedding", exception);
        }
    }

    private double cosine(double[] left, double[] right) {
        if (left.length != right.length) return -1;
        double dot = 0, leftLength = 0, rightLength = 0;
        for (int index = 0; index < left.length; index++) {
            dot += left[index] * right[index];
            leftLength += left[index] * left[index];
            rightLength += right[index] * right[index];
        }
        if (leftLength == 0 || rightLength == 0) return -1;
        return Math.max(-1, Math.min(1, dot / (Math.sqrt(leftLength) * Math.sqrt(rightLength))));
    }

    private record SourceKey(String type, Long id) { }
    private record Candidate(Long id, double semantic, double score) { }
    public record Result(IndexedChunk chunk, double semantic) { }
}
