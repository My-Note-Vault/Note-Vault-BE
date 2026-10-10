package com.example.search.retrieval;

import com.notevault.workspace.api.search.SearchSourceRef;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Request-local observations of the real retrieval, returned only by the local evaluation API. */
public record RetrievalTrace(
        Map<String, Object> settings, Map<String, Long> timingsMs,
        List<SemanticWindow> semanticWindows, List<Document> documents,
        List<Chunk> chunks, List<Selection> selected) {

    public record Signal(Double score, Integer rank, double contribution, String status) { }
    public record Document(SearchSourceRef source, Signal titleSemantic, Signal titleKeyword,
                           Signal bodySemantic, Signal bodyKeyword, Double fusedScore,
                           Integer fusedRank, String selection) { }
    public record SemanticWindow(String field, int requestedLimit, int returnedHits,
                                 int retainedDocuments) { }
    public record Chunk(SearchSourceRef source, Long chunkId, int documentPosition,
                        Double similarity, double keywordScore, double selectionScore,
                        boolean withinDocumentLimit, List<String> matchedKeywords, String excerpt) { }
    public record Selection(int contextNumber, SearchSourceRef source, Long chunkId,
                            Double similarity, String origin) { }

    public static final class Collector {
        private final Map<String, Object> settings = new LinkedHashMap<>();
        private final Map<String, Long> timings = new LinkedHashMap<>();
        private final List<SemanticWindow> windows = new ArrayList<>();
        private final Map<SearchSourceRef, Map<String, Signal>> signals = new LinkedHashMap<>();
        private final Map<SearchSourceRef, String> decisions = new LinkedHashMap<>();
        private final List<Chunk> chunks = new ArrayList<>();

        public void setting(String name, Object value) { settings.put(name, value); }
        public void timing(String name, long started) {
            timings.put(name, (System.nanoTime() - started) / 1_000_000);
        }

        public void semanticWindow(String field, int limit, int hits, int retained) {
            windows.add(new SemanticWindow(field, limit, hits, retained));
        }

        /** Scores outside a bounded ANN window are unknown, never reported as zero. */
        public void semanticScores(String field, Map<SearchSourceRef, Double> scores, double minimum) {
            scores.forEach((source, score) -> signals.computeIfAbsent(source, ignored -> new LinkedHashMap<>())
                    .put(field, new Signal(score, null, 0,
                            score < minimum ? "BELOW_THRESHOLD" : "OUTSIDE_CANDIDATE_LIMIT")));
        }

        public void ranking(String field, Map<SearchSourceRef, Double> scores, double weight, int constant) {
            Map<SearchSourceRef, Integer> ranks = WeightedRrf.ranks(scores);
            scores.forEach((source, score) -> signals.computeIfAbsent(source, ignored -> new LinkedHashMap<>())
                    .put(field, new Signal(score, ranks.get(source),
                            weight / ((double) constant + ranks.get(source)), "RETAINED")));
        }

        public void decision(SearchSourceRef source, String decision) { decisions.put(source, decision); }
        public void chunk(Chunk chunk) { chunks.add(chunk); }

        public RetrievalTrace finish(Map<SearchSourceRef, Double> fused, List<SearchSourceRef> ranked,
                                     List<SearchEvidence> evidence) {
            Map<SearchSourceRef, Integer> positions = new LinkedHashMap<>();
            for (int i = 0; i < ranked.size(); i++) positions.put(ranked.get(i), i + 1);
            List<Document> documents = new ArrayList<>();
            signals.forEach((source, fields) -> documents.add(new Document(source,
                    signal(fields, "titleSemantic"), signal(fields, "titleKeyword"),
                    signal(fields, "bodySemantic"), signal(fields, "bodyKeyword"),
                    fused.get(source), positions.get(source), decisions.getOrDefault(source,
                    fused.containsKey(source) ? "OUTSIDE_DOCUMENT_LIMIT" : "NOT_IN_FUSION"))));
            documents.sort(Comparator.comparing(Document::fusedRank, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(d -> d.source().sourceType().name())
                    .thenComparing(d -> d.source().sourceId()).thenComparing(d -> d.source().version())
                    .thenComparing(d -> d.source().title()));
            List<Selection> selected = new ArrayList<>();
            for (int i = 0; i < evidence.size(); i++) {
                SearchEvidence item = evidence.get(i);
                selected.add(new Selection(i + 1, item.source(), item.chunkId(), item.similarity(),
                        item.chunkId() == null ? "ORIGINAL_EXCERPT" : "INDEXED_CHUNK"));
            }
            return new RetrievalTrace(Map.copyOf(settings), Map.copyOf(timings), List.copyOf(windows),
                    List.copyOf(documents), List.copyOf(chunks), List.copyOf(selected));
        }

        private Signal signal(Map<String, Signal> fields, String name) {
            return fields.getOrDefault(name, new Signal(null, null, 0, "NOT_RETURNED"));
        }
    }
}
