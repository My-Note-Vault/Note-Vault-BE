package com.example.search.retrieval;

import com.notevault.workspace.api.search.KeywordSourceType;
import com.notevault.workspace.api.search.SearchSourceRef;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** PostgreSQL compares vectors; only candidate scores and selected content leave the database. */
@RequiredArgsConstructor
@Repository
public class IndexedChunkReader {
    private final NamedParameterJdbcTemplate jdbc;

    @Value("${openai.chat.semantic-chunk-candidate-limit:1000}")
    private int chunkCandidateLimit;
    @Value("${openai.chat.hnsw-ef-search:100}")
    private int efSearch;

    private static final String ACCESSIBLE_SOURCES = """
            WITH accessible AS MATERIALIZED (
                SELECT 'DOCUMENT' AS source_type, d.id AS source_id,
                       COALESCE(d.title, '') AS title,
                       CAST(d.search_revision AS VARCHAR) AS version,
                       CASE WHEN d.type = 'WORKSPACE_HOME' THEN d.workspace_id ELSE d.id END AS resource_id,
                       CASE WHEN d.type = 'WORKSPACE_HOME' THEN 'space' ELSE LOWER(d.type) END AS resource_type
                FROM document d
                WHERE EXISTS (SELECT 1 FROM workspace_member wm
                              WHERE wm.workspace_id = d.workspace_id AND wm.member_id = :memberId)
                UNION ALL
                SELECT 'DAILY_NOTE', n.id, CAST(n.logical_date AS VARCHAR),
                       encode(sha256(convert_to(
                           COALESCE(n.content, '') || COALESCE((
                               SELECT string_agg(E'\\n\\n' || p.content, '' ORDER BY p.id)
                               FROM daily_note_plan dnp JOIN plan p ON p.id=dnp.plan_id
                               WHERE dnp.daily_note_id=n.id AND p.content IS NOT NULL AND p.content <> ''
                           ), ''), 'UTF8')), 'hex'), n.id, 'daily'
                FROM daily_note n WHERE n.author_id = :memberId
            )
            """;

    // Direct distance ORDER BY + LIMIT allows HNSW. Access/freshness filters precede LIMIT.
    private static final String SIMILAR_CHUNKS = ACCESSIBLE_SOURCES + """
            , nearest AS MATERIALIZED (
                SELECT c.source_type, c.source_id, c.source_version,
                       c.embedding <=> CAST(:query AS vector(1536)) AS distance
                FROM content_chunk c
                WHERE c.embedding_status = 'READY' AND c.embedding_model = :model
                  AND c.embedding IS NOT NULL
                  AND EXISTS (
                      SELECT 1 FROM accessible a
                      WHERE a.source_type = c.source_type AND a.source_id = c.source_id
                        AND a.version = c.source_version
                  )
                ORDER BY c.embedding <=> CAST(:query AS vector(1536))
                LIMIT :limit
            )
            SELECT n.source_type, n.source_id, a.version AS source_version, a.title AS source_title,
                   1 - n.distance AS similarity
            FROM nearest n JOIN accessible a
              ON a.source_type = n.source_type AND a.source_id = n.source_id
             AND a.version = n.source_version
            ORDER BY n.distance, n.source_type, n.source_id
            """;

    @Transactional(readOnly = true)
    public Map<SearchSourceRef, Double> findSimilarTitles(
            Long memberId, String model, String query, double minSimilarity, int limit) {
        return findSimilarTitles(memberId, model, query, minSimilarity, limit, null);
    }

    @Transactional(readOnly = true)
    public Map<SearchSourceRef, Double> findSimilarTitles(
            Long memberId, String model, String query, double minSimilarity, int limit,
            RetrievalTrace.Collector trace) {
        configureVectorSearch();
        Map<String, Object> parameters = parameters(memberId, model, query);
        parameters.put("limit", limit);
        List<SemanticHit> hits = jdbc.query(ACCESSIBLE_SOURCES + """
                , nearest AS MATERIALIZED (
                    SELECT t.source_type, t.source_id, t.source_title,
                           t.embedding <=> CAST(:query AS vector(1536)) AS distance
                    FROM content_title_embedding t
                    WHERE t.embedding_status = 'READY' AND t.embedding_model = :model
                      AND t.embedding IS NOT NULL
                      AND EXISTS (
                          SELECT 1 FROM accessible a
                          WHERE a.source_type = t.source_type AND a.source_id = t.source_id
                            AND a.title = t.source_title
                      )
                    ORDER BY t.embedding <=> CAST(:query AS vector(1536))
                    LIMIT :limit
                )
                SELECT n.source_type, n.source_id, a.version AS source_version, a.title AS source_title,
                       1 - n.distance AS similarity
                FROM nearest n JOIN accessible a
                  ON a.source_type = n.source_type AND a.source_id = n.source_id
                 AND a.title = n.source_title
                ORDER BY n.distance, n.source_type, n.source_id
                """, parameters, (rows, row) -> readHit(rows));
        Map<SearchSourceRef, Double> ranked = rankDocuments(hits, minSimilarity, limit);
        observeSemantic(trace, "titleSemantic", hits, minSimilarity, limit, ranked.size());
        return ranked;
    }

    @Transactional(readOnly = true)
    public Map<SearchSourceRef, Double> findSimilarDocuments(
            Long memberId, String model, String query, double minSimilarity, int limit) {
        return findSimilarDocuments(memberId, model, query, minSimilarity, limit, null);
    }

    @Transactional(readOnly = true)
    public Map<SearchSourceRef, Double> findSimilarDocuments(
            Long memberId, String model, String query, double minSimilarity, int limit,
            RetrievalTrace.Collector trace) {
        if (chunkCandidateLimit < limit || chunkCandidateLimit > 10000) {
            throw new IllegalArgumentException("semantic-chunk-candidate-limit must be between candidate-limit and 10000");
        }
        configureVectorSearch();
        Map<String, Object> parameters = parameters(memberId, model, query);
        int chunkLimit = Math.min(chunkCandidateLimit, limit * 4);
        while (true) {
            parameters.put("limit", chunkLimit);
            List<SemanticHit> hits = jdbc.query(SIMILAR_CHUNKS, parameters, (rows, row) -> readHit(rows));
            Map<SearchSourceRef, Double> ranked = rankDocuments(hits, minSimilarity, limit);
            observeSemantic(trace, "bodySemantic", hits, minSimilarity, chunkLimit, ranked.size());
            // Expand a bounded window if one long document occupies many chunk positions.
            if (ranked.size() >= limit || hits.size() < chunkLimit || chunkLimit == chunkCandidateLimit
                    || hits.getLast().similarity() < minSimilarity) {
                return ranked;
            }
            chunkLimit = Math.min(chunkCandidateLimit, chunkLimit * 2);
        }
    }

    /** Exact reranking only within selected documents, including keyword-only/title-only hits. */
    @Transactional(readOnly = true)
    public List<IndexedChunk> findBestChunks(Long memberId, String model, String query,
                                            String question, List<String> keywords,
                                            List<SearchSourceRef> sources, double minSimilarity, int limit) {
        return findBestChunks(memberId, model, query, question, keywords, sources, minSimilarity, limit, null);
    }

    @Transactional(readOnly = true)
    public List<IndexedChunk> findBestChunks(Long memberId, String model, String query,
                                            String question, List<String> keywords,
                                            List<SearchSourceRef> sources, double minSimilarity, int limit,
                                            RetrievalTrace.Collector trace) {
        if (sources.isEmpty()) return List.of();
        Map<String, Object> parameters = parameters(memberId, model, query);
        parameters.put("minSimilarity", minSimilarity);
        parameters.put("limit", limit);
        parameters.put("includeDiagnostics", trace != null);
        List<String> requested = new ArrayList<>();
        for (int index = 0; index < sources.size(); index++) {
            SearchSourceRef source = sources.get(index);
            parameters.put("type" + index, source.sourceType().name());
            parameters.put("id" + index, source.sourceId());
            parameters.put("version" + index, source.version());
            parameters.put("title" + index, source.title());
            requested.add("(:type%d, CAST(:id%d AS BIGINT), :version%d, :title%d)"
                    .formatted(index, index, index, index));
        }
        String keywordScore = keywordScore(question, keywords, parameters);
        String sql = ACCESSIBLE_SOURCES + """
                , requested(source_type, source_id, version, title) AS (VALUES %s), scored AS (
                    SELECT c.id, c.source_type, c.source_id, a.resource_id, a.resource_type,
                           a.title AS source_title, a.version AS source_version, c.content,
                           CASE WHEN c.embedding_status = 'READY' AND c.embedding_model = :model
                                THEN 1 - (c.embedding <=> CAST(:query AS vector(1536))) END AS similarity,
                           %s AS keyword_score
                    FROM content_chunk c
                    JOIN requested r ON r.source_type = c.source_type AND r.source_id = c.source_id
                                    AND r.version = c.source_version
                    JOIN accessible a ON a.source_type = r.source_type AND a.source_id = r.source_id
                                     AND a.version = r.version AND a.title = r.title
                    WHERE c.content IS NOT NULL AND BTRIM(c.content) <> ''
                ), combined AS (
                    SELECT *, (0.7 * GREATEST(0, LEAST(1,
                                      (COALESCE(similarity, :minSimilarity) - :minSimilarity)
                                      / (1 - :minSimilarity)))
                                  + 0.3 * LEAST(1, keyword_score / 4.0)) AS selection_score
                    FROM scored
                ), ranked AS (
                    SELECT *, ROW_NUMBER() OVER (
                        PARTITION BY source_type, source_id
                        ORDER BY selection_score DESC,
                                 similarity DESC NULLS LAST, id
                    ) AS position
                    FROM combined
                )
                SELECT id, source_type, source_id, resource_id, resource_type,
                       source_title, source_version, content, similarity,
                       keyword_score, selection_score, position
                FROM ranked WHERE :includeDiagnostics OR position <= :limit
                ORDER BY source_type, source_id, position
                """.formatted(String.join(", ", requested), keywordScore);
        List<IndexedChunk> selected = new ArrayList<>();
        jdbc.query(sql, parameters, (org.springframework.jdbc.core.RowCallbackHandler) rows -> {
            IndexedChunk chunk = new IndexedChunk(rows.getLong("id"), readSource(rows),
                    rows.getLong("resource_id"), rows.getString("resource_type"), rows.getString("content"),
                    rows.getObject("similarity", Double.class));
            int position = rows.getInt("position");
            if (position <= limit) selected.add(chunk);
            if (trace != null) {
                String lowerContent = chunk.content().toLowerCase(Locale.ROOT);
                List<String> matched = keywords.stream().filter(Objects::nonNull).map(String::strip)
                        .filter(term -> !term.isEmpty() && lowerContent.contains(term.toLowerCase(Locale.ROOT)))
                        .distinct().toList();
                String excerpt = chunk.content().length() <= 300 ? chunk.content() : chunk.content().substring(0, 300) + "…";
                trace.chunk(new RetrievalTrace.Chunk(chunk.source(), chunk.id(), position, chunk.similarity(),
                        rows.getDouble("keyword_score"), rows.getDouble("selection_score"),
                        position <= limit, matched, excerpt));
            }
        });
        return List.copyOf(selected);
    }

    private void observeSemantic(RetrievalTrace.Collector trace, String field, List<SemanticHit> hits,
                                 double minimum, int limit, int retained) {
        if (trace == null) return;
        trace.setting("semanticChunkCandidateLimit", chunkCandidateLimit);
        trace.setting("hnswEfSearch", efSearch);
        Map<SearchSourceRef, Double> scores = new LinkedHashMap<>();
        for (SemanticHit hit : hits) scores.merge(hit.source(), hit.similarity(), Math::max);
        trace.semanticScores(field, scores, minimum);
        trace.semanticWindow(field, limit, hits.size(), retained);
    }

    private void configureVectorSearch() {
        if (efSearch < 1 || efSearch > 1000) throw new IllegalArgumentException("hnsw-ef-search must be between 1 and 1000");
        // Transaction-local settings cannot leak to the next request on this pooled connection.
        // pgvector >= 0.8 can continue traversing when access/model/version filters discard neighbors.
        jdbc.queryForMap("""
                SELECT set_config('hnsw.iterative_scan', 'strict_order', true) AS iterative_scan,
                       set_config('hnsw.ef_search', :efSearch, true) AS ef_search
                """, Map.of("efSearch", Integer.toString(efSearch)));
    }

    private Map<SearchSourceRef, Double> rankDocuments(List<SemanticHit> hits, double minimum, int limit) {
        Map<SearchSourceRef, Double> result = new LinkedHashMap<>();
        for (SemanticHit hit : hits) {
            if (hit.similarity() >= minimum) result.merge(hit.source(), hit.similarity(), Math::max);
            if (result.size() == limit) break;
        }
        return result;
    }

    // Same field score as KeywordSearchReader: exact question, phrase, then matched-term ratio.
    private String keywordScore(String question, List<String> keywords, Map<String, Object> parameters) {
        parameters.put("question", question.strip());
        parameters.put("phrase", "%" + escapeLike(question.strip()) + "%");
        parameters.put("escape", "\\");
        List<String> terms = keywords.stream().filter(Objects::nonNull).map(String::strip)
                .filter(term -> !term.isEmpty()).map(term -> term.toLowerCase(Locale.ROOT)).distinct().toList();
        List<String> counts = new ArrayList<>();
        for (int index = 0; index < terms.size(); index++) {
            parameters.put("term" + index, "%" + escapeLike(terms.get(index)) + "%");
            counts.add("CASE WHEN c.content ILIKE :term" + index + " ESCAPE :escape THEN 1 ELSE 0 END");
        }
        parameters.put("denominator", Math.max(1, terms.size()));
        return """
                (CASE WHEN LOWER(c.content) = LOWER(:question) THEN 3.0
                      WHEN c.content ILIKE :phrase ESCAPE :escape THEN 2.0 ELSE 0.0 END)
                + (%s) * 1.0 / :denominator
                """.formatted(counts.isEmpty() ? "0" : String.join(" + ", counts));
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private Map<String, Object> parameters(Long memberId, String model, String query) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("memberId", memberId);
        parameters.put("model", model);
        parameters.put("query", query);
        return parameters;
    }

    private SemanticHit readHit(ResultSet rows) throws SQLException {
        return new SemanticHit(readSource(rows), rows.getDouble("similarity"));
    }

    private SearchSourceRef readSource(ResultSet rows) throws SQLException {
        return new SearchSourceRef(KeywordSourceType.valueOf(rows.getString("source_type")),
                rows.getLong("source_id"), rows.getString("source_version"), rows.getString("source_title"));
    }

    private record SemanticHit(SearchSourceRef source, double similarity) { }
}
