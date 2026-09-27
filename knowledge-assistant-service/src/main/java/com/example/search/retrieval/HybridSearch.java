package com.example.search.retrieval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notevault.workspace.api.search.FieldKeywordHit;
import com.notevault.workspace.api.search.KeywordSearchReader;
import com.notevault.workspace.api.search.SearchSourceContent;
import com.notevault.workspace.api.search.SearchSourceRef;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HybridSearch {
    private final IndexedChunkReader reader;
    private final KeywordSearchReader keywords;
    private final ObjectMapper mapper;
    private final SourceExcerptSelector excerpts;

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

    public List<SearchEvidence> search(Long memberId, String question, List<String> terms,
                               String model, String questionEmbedding, int topK) {
        if (topK < 1 || documentTopK < 1 || chunksPerDocument < 1) {
            throw new IllegalArgumentException("Search result counts must be positive");
        }
        if (!Double.isFinite(minSimilarity) || minSimilarity < -1 || minSimilarity >= 1
                || !Double.isFinite(titleMinSimilarity) || titleMinSimilarity < -1 || titleMinSimilarity > 1) {
            throw new IllegalArgumentException("Invalid similarity threshold");
        }
        double[] query = parse(questionEmbedding);
        Map<SearchSourceRef, Double> titleKeyword = new HashMap<>();
        Map<SearchSourceRef, Double> originalKeyword = new HashMap<>();
        Map<Long, FieldKeywordHit> chunkKeyword = new HashMap<>();
        keywords.scanHybridMatches(memberId, question, terms, hit -> {
            if (hit.matchedField() == FieldKeywordHit.MatchedField.TITLE) {
                titleKeyword.put(hit.source(), hit.score());
            } else if (hit.chunkId() == null) {
                originalKeyword.put(hit.source(), hit.score());
            } else {
                chunkKeyword.put(hit.chunkId(), hit);
            }
        });

        Map<SearchSourceRef, Double> titleSemantic = new HashMap<>();
        reader.scanAccessibleTitles(memberId, model, title -> {
            double similarity = cosine(query, parse(title.embedding()));
            if (similarity >= titleMinSimilarity) {
                titleSemantic.put(title.source(), similarity);
            }
        });

        Map<SearchSourceRef, Double> bodySemantic = new HashMap<>();
        Map<SearchSourceRef, Double> bodyKeyword = new HashMap<>(originalKeyword);
        Map<SearchSourceRef, List<Candidate>> bestChunks = new HashMap<>();
        Comparator<Candidate> chunkOrder = Comparator.comparingDouble(Candidate::score).reversed()
                .thenComparing(Candidate::semantic, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparingLong(Candidate::id);

        // Every current chunk is visited. Retain IDs/scores only, not the full corpus or vectors.
        reader.scanAccessibleChunks(memberId, model, chunk -> {
            SearchSourceRef source = chunk.source();
            FieldKeywordHit matched = chunkKeyword.remove(chunk.id());
            double keyword = matched != null && matched.source().equals(source) ? matched.score() : 0;
            Double semantic = chunk.embedding() == null ? null : cosine(query, parse(chunk.embedding()));
            if (semantic != null && semantic >= minSimilarity) {
                bodySemantic.merge(source, semantic, Math::max);
            }
            if (keyword > 0) bodyKeyword.merge(source, keyword, Math::max);
            double normalized = semantic == null ? 0
                    : Math.max(0, Math.min(1, (semantic - minSimilarity) / (1 - minSimilarity)));
            double score = 0.7 * normalized + 0.3 * Math.min(1, keyword / 4.0);
            List<Candidate> best = bestChunks.computeIfAbsent(source, ignored -> new ArrayList<>());
            best.add(new Candidate(chunk.id(), semantic, score));
            best.sort(chunkOrder);
            if (best.size() > chunksPerDocument) best.removeLast();
        });

        Map<SearchSourceRef, Double> fused = WeightedRrf.fuse(List.of(
                new WeightedRrf.Ranking<>(titleSemantic, titleSemanticWeight),
                new WeightedRrf.Ranking<>(titleKeyword, titleKeywordWeight),
                new WeightedRrf.Ranking<>(bodySemantic, bodySemanticWeight),
                new WeightedRrf.Ranking<>(bodyKeyword, bodyKeywordWeight)), rankConstant);
        List<SearchSourceRef> ranked = fused.entrySet().stream()
                .sorted(Map.Entry.<SearchSourceRef, Double>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().sourceType().name())
                        .thenComparing(entry -> entry.getKey().sourceId())
                        .thenComparing(entry -> entry.getKey().version())
                        .thenComparing(entry -> entry.getKey().title()))
                .map(Map.Entry::getKey).toList();
        Map<SearchSourceRef, List<SearchEvidence>> documents = new LinkedHashMap<>();
        int target = Math.min(documentTopK, topK);
        int cursor = 0;
        while (documents.size() < target && cursor < ranked.size()) {
            int end = Math.min(ranked.size(), cursor + target - documents.size());
            List<SearchSourceRef> batch = ranked.subList(cursor, end);
            Map<SearchSourceRef, List<SearchEvidence>> loaded = loadEvidence(
                    memberId, question, terms, model, batch, bestChunks, originalKeyword);
            for (SearchSourceRef source : batch) {
                List<SearchEvidence> evidence = loaded.getOrDefault(source, List.of());
                if (!evidence.isEmpty() && documents.keySet().stream().noneMatch(source::sameSource)) {
                    documents.put(source, evidence);
                }
            }
            cursor = end;
        }

        List<SearchEvidence> selected = new ArrayList<>();
        for (int position = 0; position < chunksPerDocument && selected.size() < topK; position++) {
            for (List<SearchEvidence> best : documents.values()) {
                if (position < best.size()) selected.add(best.get(position));
                if (selected.size() == topK) break;
            }
        }
        return List.copyOf(selected);
    }

    private Map<SearchSourceRef, List<SearchEvidence>> loadEvidence(
            Long memberId, String question, List<String> terms, String model, List<SearchSourceRef> sources,
            Map<SearchSourceRef, List<Candidate>> bestChunks, Map<SearchSourceRef, Double> originalKeyword) {
        // A body matched before chunks existed: keep that original-text match even if the Worker
        // creates chunks between scans. Assigning its score to arbitrary new chunks would lose context.
        List<Long> chunkIds = sources.stream().filter(source -> !originalKeyword.containsKey(source))
                .flatMap(source -> bestChunks.getOrDefault(source, List.of()).stream())
                .map(Candidate::id).toList();
        Map<Long, IndexedChunk> chunks = new HashMap<>();
        reader.findAccessibleByIds(memberId, model, chunkIds).forEach(chunk -> chunks.put(chunk.id(), chunk));
        Map<SearchSourceRef, List<SearchEvidence>> result = new HashMap<>();
        List<SearchSourceRef> originals = new ArrayList<>();
        for (SearchSourceRef source : sources) {
            List<SearchEvidence> evidence = new ArrayList<>();
            for (Candidate candidate : bestChunks.getOrDefault(source, List.of())) {
                IndexedChunk chunk = chunks.get(candidate.id());
                if (chunk != null && chunk.source().equals(source) && !chunk.content().isBlank()) {
                    evidence.add(SearchEvidence.fromChunk(chunk,
                            chunk.embedding() == null ? null : candidate.semantic()));
                }
            }
            if (evidence.isEmpty()) originals.add(source);
            else result.put(source, evidence);
        }
        for (SearchSourceContent source : keywords.findAccessibleSources(memberId, originals)) {
            List<SearchEvidence> evidence = excerpts.select(source.content(), question, terms, chunksPerDocument)
                    .stream().map(excerpt -> SearchEvidence.fromSource(source, excerpt)).toList();
            if (!evidence.isEmpty()) result.put(source.source(), evidence);
        }
        return result;
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

    private record Candidate(Long id, Double semantic, double score) { }
}
