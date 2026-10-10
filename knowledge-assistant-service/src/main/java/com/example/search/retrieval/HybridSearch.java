package com.example.search.retrieval;

import com.notevault.workspace.api.search.FieldKeywordHit;
import com.notevault.workspace.api.search.KeywordSearchReader;
import com.notevault.workspace.api.search.SearchSourceContent;
import com.notevault.workspace.api.search.SearchSourceRef;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HybridSearch {
    private final IndexedChunkReader reader;
    private final KeywordSearchReader keywords;
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
    @Value("${openai.chat.candidate-limit:50}")
    private int candidateLimit;

    public List<SearchEvidence> search(Long memberId, String question, List<String> terms,
                                       String model, String questionEmbedding, int topK) {
        return search(memberId, question, terms, model, questionEmbedding, topK, null).evidence();
    }

    public SearchResult searchWithTrace(Long memberId, String question, List<String> terms,
                                        String model, String questionEmbedding, int topK) {
        return search(memberId, question, terms, model, questionEmbedding, topK, new RetrievalTrace.Collector());
    }

    public record SearchResult(List<SearchEvidence> evidence, RetrievalTrace trace) { }

    private SearchResult search(Long memberId, String question, List<String> terms,
                                String model, String questionEmbedding, int topK, RetrievalTrace.Collector trace) {
        long started = System.nanoTime();
        if (topK < 1 || documentTopK < 1 || chunksPerDocument < 1) {
            throw new IllegalArgumentException("Search result counts must be positive");
        }
        if (candidateLimit < documentTopK || candidateLimit > 1000) {
            throw new IllegalArgumentException("candidate-limit must be between document-top-k and 1000");
        }
        if (!Double.isFinite(minSimilarity) || minSimilarity < -1 || minSimilarity >= 1
                || !Double.isFinite(titleMinSimilarity) || titleMinSimilarity < -1 || titleMinSimilarity > 1) {
            throw new IllegalArgumentException("Invalid similarity threshold");
        }
        Map<SearchSourceRef, Double> titleKeyword = new HashMap<>();
        Map<SearchSourceRef, Double> bodyKeyword = new HashMap<>();
        Set<SearchSourceRef> originalKeyword = new HashSet<>();
        long phaseStarted = System.nanoTime();
        for (FieldKeywordHit hit : keywords.findHybridMatches(memberId, question, terms, candidateLimit)) {
            if (hit.matchedField() == FieldKeywordHit.MatchedField.TITLE) {
                titleKeyword.put(hit.source(), hit.score());
            } else {
                bodyKeyword.put(hit.source(), hit.score());
                if (hit.chunkId() == null) originalKeyword.add(hit.source());
            }
        }
        if (trace != null) trace.timing("keywordSearch", phaseStarted);

        // Semantic retrieval is independent of LIKE matches. No stored vectors are read into Java.
        phaseStarted = System.nanoTime();
        Map<SearchSourceRef, Double> titleSemantic = trace == null
                ? reader.findSimilarTitles(memberId, model, questionEmbedding, titleMinSimilarity, candidateLimit)
                : reader.findSimilarTitles(memberId, model, questionEmbedding, titleMinSimilarity, candidateLimit, trace);
        if (trace != null) trace.timing("titleSemanticSearch", phaseStarted);
        phaseStarted = System.nanoTime();
        Map<SearchSourceRef, Double> bodySemantic = trace == null
                ? reader.findSimilarDocuments(memberId, model, questionEmbedding, minSimilarity, candidateLimit)
                : reader.findSimilarDocuments(memberId, model, questionEmbedding, minSimilarity, candidateLimit, trace);
        if (trace != null) trace.timing("bodySemanticSearch", phaseStarted);
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
        if (trace != null) {
            trace.setting("topK", topK);
            trace.setting("documentTopK", documentTopK);
            trace.setting("chunksPerDocument", chunksPerDocument);
            trace.setting("candidateLimit", candidateLimit);
            trace.setting("minSimilarity", minSimilarity);
            trace.setting("titleMinSimilarity", titleMinSimilarity);
            trace.setting("rrfRankConstant", rankConstant);
            trace.setting("titleSemanticWeight", titleSemanticWeight);
            trace.setting("titleKeywordWeight", titleKeywordWeight);
            trace.setting("bodySemanticWeight", bodySemanticWeight);
            trace.setting("bodyKeywordWeight", bodyKeywordWeight);
            trace.setting("chunkSemanticWeight", 0.7);
            trace.setting("chunkKeywordWeight", 0.3);
            trace.ranking("titleSemantic", titleSemantic, titleSemanticWeight, rankConstant);
            trace.ranking("titleKeyword", titleKeyword, titleKeywordWeight, rankConstant);
            trace.ranking("bodySemantic", bodySemantic, bodySemanticWeight, rankConstant);
            trace.ranking("bodyKeyword", bodyKeyword, bodyKeywordWeight, rankConstant);
        }
        Map<SearchSourceRef, List<SearchEvidence>> documents = new LinkedHashMap<>();
        int target = Math.min(documentTopK, topK);
        int cursor = 0;
        phaseStarted = System.nanoTime();
        while (documents.size() < target && cursor < ranked.size()) {
            int end = Math.min(ranked.size(), cursor + target - documents.size());
            List<SearchSourceRef> batch = ranked.subList(cursor, end);
            Map<SearchSourceRef, List<SearchEvidence>> loaded = loadEvidence(
                    memberId, question, terms, model, questionEmbedding, batch, originalKeyword, trace);
            for (SearchSourceRef source : batch) {
                List<SearchEvidence> evidence = loaded.getOrDefault(source, List.of());
                boolean duplicate = documents.keySet().stream().anyMatch(source::sameSource);
                if (!evidence.isEmpty() && !duplicate) {
                    documents.put(source, evidence);
                }
                if (trace != null) trace.decision(source, evidence.isEmpty() ? "NO_EVIDENCE"
                        : duplicate ? "DUPLICATE_SOURCE" : "SELECTED");
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
        if (trace != null) {
            trace.timing("evidenceSelection", phaseStarted);
            trace.timing("total", started);
        }
        return new SearchResult(List.copyOf(selected), trace == null ? null : trace.finish(fused, ranked, selected));
    }

    private Map<SearchSourceRef, List<SearchEvidence>> loadEvidence(
            Long memberId, String question, List<String> terms, String model, String questionEmbedding,
            List<SearchSourceRef> sources, Set<SearchSourceRef> originalKeyword, RetrievalTrace.Collector trace) {
        // Preserve a raw-text match even if the Worker creates its chunks during this request.
        List<SearchSourceRef> indexed = sources.stream().filter(source -> !originalKeyword.contains(source)).toList();
        Map<SearchSourceRef, List<SearchEvidence>> result = new HashMap<>();
        List<IndexedChunk> chunks = trace == null
                ? reader.findBestChunks(memberId, model, questionEmbedding, question, terms, indexed,
                        minSimilarity, chunksPerDocument)
                : reader.findBestChunks(memberId, model, questionEmbedding, question, terms, indexed,
                        minSimilarity, chunksPerDocument, trace);
        for (IndexedChunk chunk : chunks) {
            if (!chunk.content().isBlank()) {
                result.computeIfAbsent(chunk.source(), ignored -> new ArrayList<>()).add(SearchEvidence.fromChunk(chunk));
            }
        }
        List<SearchSourceRef> originals = sources.stream().filter(source -> !result.containsKey(source)).toList();
        for (SearchSourceContent source : keywords.findAccessibleSources(memberId, originals)) {
            List<SearchEvidence> evidence = excerpts.select(source.content(), question, terms, chunksPerDocument)
                    .stream().map(excerpt -> SearchEvidence.fromSource(source, excerpt)).toList();
            if (!evidence.isEmpty()) result.put(source.source(), evidence);
        }
        return result;
    }
}
